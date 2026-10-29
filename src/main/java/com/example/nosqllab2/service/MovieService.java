package com.example.nosqllab2.service;

import com.example.nosqllab2.dto.request.create.CreateMovieDto;
import com.example.nosqllab2.dto.request.update.UpdateMovieDto;
import com.example.nosqllab2.dto.response.MovieResponseDto;
import com.example.nosqllab2.dto.response.schedule.EditingNoticeDto;
import com.example.nosqllab2.dto.response.schedule.MovieScheduleDto;
import com.example.nosqllab2.dto.response.schedule.ScheduleResponseDto;
import com.example.nosqllab2.entity.Category;
import com.example.nosqllab2.entity.Movie;
import com.example.nosqllab2.exception.custom.EntityNotFoundException;
import com.example.nosqllab2.exception.custom.ScheduleConflictException;
import com.example.nosqllab2.mapper.MovieMapper;
import com.example.nosqllab2.repository.MovieRepository;
import com.example.nosqllab2.util.MovieScheduleUtils;
import com.example.nosqllab2.util.ShowDateParser;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class MovieService {

    private static final Duration LOCK_TTL = Duration.ofSeconds(300);
    private static final String MOVIES_CACHE_KEY = "movies:all";
    private static final String EDITING_FLAG_PREFIX = "editing:";

    private final MovieRepository movieRepository;
    private final CategoryService categoryService;
    private final NotificationService notificationService;
    private final RedisService redisService;
    private final RedisLockService redisLockService;
    private final MovieMapper movieMapper;
    private final ObjectMapper objectMapper;
    private final Duration propagationDelay;

    public MovieService(
            MovieRepository movieRepository,
            CategoryService categoryService,
            NotificationService notificationService,
            RedisService redisService,
            RedisLockService redisLockService,
            MovieMapper movieMapper,
            ObjectMapper objectMapper,
            @Value("${app.schedule.propagation-delay-ms}") long propagationDelayMs
    ) {
        this.movieRepository = movieRepository;
        this.categoryService = categoryService;
        this.notificationService = notificationService;
        this.redisService = redisService;
        this.redisLockService = redisLockService;
        this.movieMapper = movieMapper;
        this.objectMapper = objectMapper;
        this.propagationDelay = Duration.ofMillis(propagationDelayMs);
    }

    /**
     * Расписание кэшируется на каждом узле по схеме cache-aside с TTL, равным задержке распространения.
     * Пока хоть один день расписания правится (жив флаг editing:*), расписание не отдаётся:
     * узел ещё не гарантирует свежие данные.
     */
    public ScheduleResponseDto findAllMovies() {
        Set<String> editingKeys = redisService.keys(EDITING_FLAG_PREFIX + "*");

        if (editingKeys != null && !editingKeys.isEmpty()) {
            return EditingNoticeDto.of();
        }

        List<MovieResponseDto> cachedMovies = readScheduleFromCache();

        if (cachedMovies != null) {
            return new MovieScheduleDto(cachedMovies);
        }

        List<MovieResponseDto> movies = movieRepository.findAll().stream()
                .map(movieMapper::toDto)
                .toList();

        writeScheduleToCache(movies);

        return new MovieScheduleDto(movies);
    }

    public MovieResponseDto findById(Long id) {
        return movieMapper.toDto(getEntityById(id));
    }

    @Transactional
    public MovieResponseDto addMovie(CreateMovieDto dto) {
        Category category = categoryService.getEntityByName(dto.category());

        LocalDateTime showDate = ShowDateParser.parse(dto.showDate());
        String dateKey = ShowDateParser.toDateKey(showDate);

        RedisLockService.ScheduleLock lock = redisLockService.acquireScheduleLock(List.of(dateKey), LOCK_TTL);

        try {
            checkTimeIsFree(showDate, dto.duration(), null);

            Movie movie = new Movie();
            movie.setTitle(dto.title());
            movie.setDescription(dto.description());
            movie.setShowDate(showDate);
            movie.setDuration(dto.duration());
            movie.setCategory(category);

            Movie savedMovie = movieRepository.save(movie);

            notificationService.scheduleNotification(
                    "Премьера дня",
                    "Сегодня в " + dto.showDate() + " показываем фильм «" + savedMovie.getTitle() + "».",
                    savedMovie.getShowDate()
            );

            redisService.delete(MOVIES_CACHE_KEY);

            return movieMapper.toDto(savedMovie);
        } finally {
            redisLockService.release(lock);
        }
    }

    @Transactional
    public MovieResponseDto updateMovie(Long id, UpdateMovieDto dto) {
        Movie movie = getEntityById(id);

        LocalDateTime showDate = ShowDateParser.parse(dto.showDate());
        int duration = dto.duration() == null ? movie.getDuration() : dto.duration();

        String oldDateKey = ShowDateParser.toDateKey(movie.getShowDate());
        String newDateKey = ShowDateParser.toDateKey(showDate);
        List<String> dateKeys = List.of(oldDateKey, newDateKey);

        RedisLockService.ScheduleLock lock = redisLockService.acquireScheduleLock(dateKeys, LOCK_TTL);

        try {
            // флаг правок уходит на чужие узлы сразу после захвата лока, ещё до записи в PostgreSQL
            redisLockService.broadcastEditingFlag(dateKeys, propagationDelay);

            checkTimeIsFree(showDate, duration, movie.getId());

            movie.setShowDate(showDate);
            movie.setDuration(duration);

            Movie savedMovie = movieRepository.save(movie);

            notificationService.scheduleNotification(
                    "Расписание изменено",
                    "Фильм «" + savedMovie.getTitle() + "» перенесён на " + dto.showDate() + ".",
                    savedMovie.getShowDate()
            );

            // узел, на котором проходило изменение, инвалидирует свой кэш сразу и видит актуальные данные без задержки
            redisService.delete(MOVIES_CACHE_KEY);

            return movieMapper.toDto(savedMovie);
        } finally {
            redisLockService.release(lock);
        }
    }

    @Transactional
    public void deleteMovie(Long id) {
        movieRepository.delete(getEntityById(id));
        redisService.delete(MOVIES_CACHE_KEY);
    }

    private Movie getEntityById(Long id) {
        Optional<Movie> optionalMovie = movieRepository.findWithCategoryById(id);

        if (optionalMovie.isEmpty()) {
            throw new EntityNotFoundException("Фильм с id " + id + " не найден");
        }

        return optionalMovie.get();
    }

    // в кинотеатре один зал, поэтому показы одного дня не должны пересекаться по времени
    private void checkTimeIsFree(LocalDateTime showDate, int duration, Long movieIdToSkip) {
        MovieScheduleUtils.DayBounds dayBounds = MovieScheduleUtils.getDayBounds(showDate);

        List<Movie> moviesOnThisDay = movieRepository
                .findAllByShowDateBetween(dayBounds.startOfDay(), dayBounds.endOfDay()).stream()
                .filter(movie -> !movie.getId().equals(movieIdToSkip))
                .toList();

        if (MovieScheduleUtils.hasTimeConflict(showDate, duration, moviesOnThisDay)) {
            throw new ScheduleConflictException();
        }
    }

    private List<MovieResponseDto> readScheduleFromCache() {
        String cached = redisService.get(MOVIES_CACHE_KEY);

        if (cached == null) {
            return null;
        }

        try {
            return objectMapper.readValue(cached, new TypeReference<List<MovieResponseDto>>() {});
        } catch (JacksonException ex) {
            redisService.delete(MOVIES_CACHE_KEY);
            return null;
        }
    }

    private void writeScheduleToCache(List<MovieResponseDto> movies) {
        try {
            redisService.set(MOVIES_CACHE_KEY, objectMapper.writeValueAsString(movies), propagationDelay);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Не удалось сериализовать расписание для кэша", ex);
        }
    }
}
