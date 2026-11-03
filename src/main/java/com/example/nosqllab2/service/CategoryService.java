package com.example.nosqllab2.service;

import com.example.nosqllab2.dto.request.create.CreateCategoryDto;
import com.example.nosqllab2.dto.response.CategoryResponseDto;
import com.example.nosqllab2.dto.response.CategoryWithMoviesDto;
import com.example.nosqllab2.document.CategoryDocument;
import com.example.nosqllab2.exception.custom.EntityAlreadyExistsException;
import com.example.nosqllab2.exception.custom.EntityNotFoundException;
import com.example.nosqllab2.mapper.CategoryMapper;
import com.example.nosqllab2.repository.firestore.CategoryDocumentRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private static final String CATEGORY_CACHE_KEY_PREFIX = "category:";
    private static final Duration CATEGORY_CACHE_TTL = Duration.ofMinutes(5);

    private final CategoryDocumentRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    public CategoryService(
            CategoryDocumentRepository categoryRepository,
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

        CategoryDocument category = new CategoryDocument();
        category.setName(dto.name());

        if (!categoryRepository.create(category)) {
            throw new EntityAlreadyExistsException("Категория с таким названием уже существует");
        }

        return categoryMapper.toDto(category);
    }

    public CategoryWithMoviesDto findById(String id) {
        String cacheKey = CATEGORY_CACHE_KEY_PREFIX + id;

        CategoryWithMoviesDto cachedCategory = readFromCache(cacheKey);

        if (cachedCategory != null) {
            return cachedCategory;
        }

        CategoryWithMoviesDto category = categoryMapper.toDtoWithMovies(getEntityById(id));

        writeToCache(cacheKey, category);

        return category;
    }

    public void deleteCache(String id) {
        redisService.delete(CATEGORY_CACHE_KEY_PREFIX + id);
    }

    public CategoryDocument getEntityByName(String name) {
        Optional<CategoryDocument> optionalCategory = categoryRepository.findByName(name);

        if (optionalCategory.isEmpty()) {
            throw new EntityNotFoundException("Категория " + name + " не найдена");
        }

        return optionalCategory.get();
    }

    private CategoryDocument getEntityById(String id) {
        Optional<CategoryDocument> optionalCategory = categoryRepository.findWithMoviesById(id);

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
