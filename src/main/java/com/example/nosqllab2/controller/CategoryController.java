package com.example.nosqllab2.controller;

import com.example.nosqllab2.dto.request.create.CreateCategoryDto;
import com.example.nosqllab2.dto.response.CategoryResponseDto;
import com.example.nosqllab2.dto.response.CategoryWithMoviesDto;
import com.example.nosqllab2.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponseDto>> getAllCategories() {
        return ResponseEntity.ok(categoryService.findAllCategories());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryWithMoviesDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(categoryService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponseDto> create(@Valid @RequestBody CreateCategoryDto dto) {
        CategoryResponseDto created = categoryService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
