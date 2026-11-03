package com.example.nosqllab2.service;

import com.example.nosqllab2.dto.request.create.CreateMovieDto;
import com.example.nosqllab2.dto.request.update.UpdateMovieDto;
import com.example.nosqllab2.dto.response.MovieResponseDto;
import com.example.nosqllab2.dto.response.schedule.EditingNoticeDto;
import com.example.nosqllab2.dto.response.schedule.MovieScheduleDto;
import com.example.nosqllab2.dto.response.schedule.ScheduleResponseDto;
import com.example.nosqllab2.document.CategoryDocument;
import com.example.nosqllab2.document.MovieDocument;
import com.example.nosqllab2.document.MovieStage;
import com.example.nosqllab2.document.MovieState;
import com.example.nosqllab2.exception.custom.EntityNotFoundException;
import com.example.nosqllab2.exception.custom.InvalidStateTransitionException;
import com.example.nosqllab2.exception.custom.ScheduleConflictException;
import com.example.nosqllab2.mapper.MovieMapper;
import com.example.nosqllab2.repository.firestore.CategoryDocumentRepository;
import com.example.nosqllab2.repository.firestore.MovieDocumentRepository;
import com.example.nosqllab2.util.MovieScheduleUtils;
import com.example.nosqllab2.util.ShowDateParser;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteBatch;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.example.nosqllab2.util.FirestoreUtils.await;

@Service
public class MovieService {

    private static final Duration LOCK_TTL = Duration.ofSeconds(300);
    private static final String MOVIES_CACHE_KEY = "movies:all";
    private static final String EDITING_FLAG_PREFIX = "editing:";
    private static final long SHOWN_CHECK_INTERVAL_MS = 60_000;

    private final Firestore firestore;
    private final MovieDocumentRepository movieRepository;
    private final CategoryDocumentRepository categoryRepository;
    private final CategoryService categoryService;
    private final NotificationService notificationService;
    private final RedisService redisService;
    private final RedisLockService redisLockService;
    private final MovieMapper movieMapper;
    private final ObjectMapper objectMapper;
    private final Duration propagationDelay;

    public MovieService(
            Firestore firestore,
            MovieDocumentRepository movieRepository,
            CategoryDocumentRepository categoryRepository,
            CategoryService categoryService,
            NotificationService notificationService,
            RedisService redisService,
            RedisLockService redisLockService,
            MovieMapper movieMapper,
            ObjectMapper objectMapper,
            @Value("${app.schedule.propagation-delay-ms}") long propagationDelayMs
    ) {
        this.firestore = firestore;
        this.movieRepository = movieRepository;
        this.categoryRepository = categoryRepository;
        this.categoryService = categoryService;
        this.notificationService = notificationService;
        this.redisService = redisService;
        this.redisLockService = redisLockService;
        this.movieMapper = movieMapper;
        this.objectMapper = objectMapper;
        this.propagationDelay = Duration.ofMillis(propagationDelayMs);
    }

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

    public MovieResponseDto findById(String id) {
        return movieMapper.toDto(getEntityById(id));
    }

    public MovieResponseDto addMovie(CreateMovieDto dto) {
        CategoryDocument category = categoryService.getEntityByName(dto.category());

        LocalDateTime showDate = ShowDateParser.parse(dto.showDate());
        String dateKey = ShowDateParser.toDateKey(showDate);

        RedisLockService.ScheduleLock lock = redisLockService.acquireScheduleLock(List.of(dateKey), LOCK_TTL);

        try {
            checkTimeIsFree(showDate, dto.duration(), null);

            MovieDocument movie = new MovieDocument();
            movie.setTitle(dto.title());
            movie.setDescription(dto.description());
            setSchedule(movie, showDate, dto.duration());
            movie.setCategoryId(category.getId());
            movie.setCategoryName(category.getName());
            movie.setState(MovieState.PLANNED);
            addStage(movie, MovieState.PLANNED);

            WriteBatch batch = firestore.batch();
            movieRepository.save(batch, movie);
            categoryRepository.putMovieCopy(batch, movie);
            await(batch.commit());

            notificationService.scheduleNotification(
                    "Премьера дня",
                    "Сегодня в " + dto.showDate() + " показываем фильм «" + movie.getTitle() + "».",
                    showDate
            );

            redisService.delete(MOVIES_CACHE_KEY);
            categoryService.deleteCache(movie.getCategoryId());

            return movieMapper.toDto(movie);
        } finally {
            redisLockService.release(lock);
        }
    }

    public MovieResponseDto updateMovie(String id, UpdateMovieDto dto) {
        MovieDocument movie = getEntityById(id);

        checkTransition(movie, MovieState.RESCHEDULED);

        LocalDateTime showDate = ShowDateParser.parse(dto.showDate());
        int duration = dto.duration() == null ? movie.getDuration() : dto.duration();

        String oldDateKey = ShowDateParser.toDateKey(ShowDateParser.fromStoredValue(movie.getShowDate()));
        String newDateKey = ShowDateParser.toDateKey(showDate);
        List<String> dateKeys = List.of(oldDateKey, newDateKey);

        RedisLockService.ScheduleLock lock = redisLockService.acquireScheduleLock(dateKeys, LOCK_TTL);

        try {
            redisLockService.broadcastEditingFlag(dateKeys, propagationDelay);

            checkTimeIsFree(showDate, duration, movie.getId());

            MovieDocument updated = await(firestore.runTransaction(tx -> {
                MovieDocument current = movieRepository.findById(tx, id)
                        .orElseThrow(() -> new EntityNotFoundException("Фильм с id " + id + " не найден"));

                checkTransition(current, MovieState.RESCHEDULED);

                setSchedule(current, showDate, duration);
                current.setState(MovieState.RESCHEDULED);
                addStage(current, MovieState.RESCHEDULED);

                movieRepository.save(tx, current);
                categoryRepository.putMovieCopy(tx, current);

                return current;
            }));

            notificationService.scheduleNotification(
                    "Расписание изменено",
                    "Фильм «" + updated.getTitle() + "» перенесён на " + dto.showDate() + ".",
                    showDate
            );

            redisService.delete(MOVIES_CACHE_KEY);
            categoryService.deleteCache(updated.getCategoryId());

            return movieMapper.toDto(updated);
        } finally {
            redisLockService.release(lock);
        }
    }

    public void deleteMovie(String id) {
        MovieDocument movie = getEntityById(id);

        WriteBatch batch = firestore.batch();
        movieRepository.delete(batch, movie);
        categoryRepository.removeMovieCopy(batch, movie);
        await(batch.commit());

        redisService.delete(MOVIES_CACHE_KEY);
        categoryService.deleteCache(movie.getCategoryId());
    }

    @Scheduled(fixedDelay = SHOWN_CHECK_INTERVAL_MS)
    public void markFinishedMoviesAsShown() {
        LocalDateTime now = LocalDateTime.now();

        List<MovieDocument> finished = movieRepository.findAllByStateInAndEndDateBefore(
                List.of(MovieState.PLANNED, MovieState.RESCHEDULED), now);

        for (MovieDocument movie : finished) {
            markAsShown(movie.getId(), now);
        }
    }

    private void markAsShown(String id, LocalDateTime now) {
        boolean changed = await(firestore.runTransaction(tx -> {
            Optional<MovieDocument> current = movieRepository.findById(tx, id);

            if (current.isEmpty() || !current.get().getState().canMoveTo(MovieState.SHOWN)
                    || ShowDateParser.fromStoredValue(current.get().getEndDate()).isAfter(now)) {
                return false;
            }

            MovieDocument movie = current.get();
            movie.setState(MovieState.SHOWN);
            addStage(movie, MovieState.SHOWN);
            movieRepository.save(tx, movie);

            return true;
        }));

        if (changed) {
            redisService.delete(MOVIES_CACHE_KEY);
        }
    }

    private static void setSchedule(MovieDocument movie, LocalDateTime showDate, int duration) {
        movie.setShowDate(ShowDateParser.toStoredValue(showDate));
        movie.setDuration(duration);
        movie.setEndDate(ShowDateParser.toStoredValue(showDate.plusMinutes(duration)));
    }

    private static void checkTransition(MovieDocument movie, MovieState next) {
        if (!movie.getState().canMoveTo(next)) {
            throw new InvalidStateTransitionException(movie.getState(), next);
        }
    }

    private static void addStage(MovieDocument movie, MovieState state) {
        MovieStage stage = new MovieStage();
        stage.setStage(state);
        stage.setAt(ShowDateParser.toStoredValue(LocalDateTime.now()));

        movie.getStages().add(stage);

        if (!movie.getStageNames().contains(state.name())) {
            movie.getStageNames().add(state.name());
        }
    }

    private MovieDocument getEntityById(String id) {
        Optional<MovieDocument> optionalMovie = movieRepository.findWithCategoryById(id);

        if (optionalMovie.isEmpty()) {
            throw new EntityNotFoundException("Фильм с id " + id + " не найден");
        }

        return optionalMovie.get();
    }

    private void checkTimeIsFree(LocalDateTime showDate, int duration, String movieIdToSkip) {
        MovieScheduleUtils.DayBounds dayBounds = MovieScheduleUtils.getDayBounds(showDate);

        List<MovieDocument> moviesOnThisDay = movieRepository
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
