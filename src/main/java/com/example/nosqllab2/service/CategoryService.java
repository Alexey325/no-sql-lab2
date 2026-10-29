package com.example.nosqllab2.service;

import com.example.nosqllab2.dto.request.create.CreateCategoryDto;
import com.example.nosqllab2.dto.response.CategoryResponseDto;
import com.example.nosqllab2.dto.response.CategoryWithMoviesDto;
import com.example.nosqllab2.entity.Category;
import com.example.nosqllab2.exception.custom.EntityAlreadyExistsException;
import com.example.nosqllab2.exception.custom.EntityNotFoundException;
import com.example.nosqllab2.mapper.CategoryMapper;
import com.example.nosqllab2.repository.CategoryRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private static final String CATEGORY_CACHE_KEY_PREFIX = "category:";
    private static final Duration CATEGORY_CACHE_TTL = Duration.ofMinutes(5);

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    public CategoryService(
            CategoryRepository categoryRepository,
            CategoryMapper categoryMapper,
            RedisService redisService,
            ObjectMapper objectMapper
    ) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
        this.redisService = redisService;
        this.objectMapper = objectMapper;
    }

    public List<CategoryResponseDto> findAllCategories() {
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toDto)
                .toList();
    }

    public CategoryResponseDto create(CreateCategoryDto dto) {
        if (categoryRepository.existsByName(dto.name())) {
            throw new EntityAlreadyExistsException("Категория с таким названием уже существует");
        }

        Category category = new Category();
        category.setName(dto.name());

        return categoryMapper.toDto(categoryRepository.save(category));
    }

    /**
     * Часто запрашиваемые категории кэшируются в redis по схеме cache-aside:
     * при первом запросе категория читается из PostgreSQL и кладётся в кэш на 5 минут,
     * при повторном отдаётся из кэша без обращения к БД.
     */
    @Transactional(readOnly = true)
    public CategoryWithMoviesDto findById(Long id) {
        String cacheKey = CATEGORY_CACHE_KEY_PREFIX + id;

        CategoryWithMoviesDto cachedCategory = readFromCache(cacheKey);

        if (cachedCategory != null) {
            return cachedCategory;
        }

        CategoryWithMoviesDto category = categoryMapper.toDtoWithMovies(getEntityById(id));

        writeToCache(cacheKey, category);

        return category;
    }

    public Category getEntityByName(String name) {
        Optional<Category> optionalCategory = categoryRepository.findByName(name);

        if (optionalCategory.isEmpty()) {
            throw new EntityNotFoundException("Категория " + name + " не найдена");
        }

        return optionalCategory.get();
    }

    private Category getEntityById(Long id) {
        Optional<Category> optionalCategory = categoryRepository.findWithMoviesById(id);

        if (optionalCategory.isEmpty()) {
            throw new EntityNotFoundException("Категория с id " + id + " не найдена");
        }

        return optionalCategory.get();
    }

    private CategoryWithMoviesDto readFromCache(String cacheKey) {
        String cached = redisService.get(cacheKey);

        if (cached == null) {
            return null;
        }

        try {
            return objectMapper.readValue(cached, CategoryWithMoviesDto.class);
        } catch (JacksonException ex) {
            // битое значение в кэше не должно ломать запрос — читаем из БД и перезапишем кэш
            redisService.delete(cacheKey);
            return null;
        }
    }

    private void writeToCache(String cacheKey, CategoryWithMoviesDto category) {
        try {
            redisService.set(cacheKey, objectMapper.writeValueAsString(category), CATEGORY_CACHE_TTL);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Не удалось сериализовать категорию для кэша", ex);
        }
    }
}
