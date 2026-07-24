package com.ooru.controller;

import com.ooru.dto.CategoryDtos.CategoryResponse;
import com.ooru.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public — this is what Home.jsx fetches to render categories, fields and all, with zero hardcoding. */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> list() {
        List<CategoryResponse> result = categoryService.allActive().stream()
                .map(categoryService::toResponse).toList();
        return ResponseEntity.ok(result);
    }
}
