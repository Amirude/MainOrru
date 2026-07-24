package com.ooru.controller;

import com.ooru.dto.CategoryDtos.*;
import com.ooru.model.Shop;
import com.ooru.service.AnalyticsService;
import com.ooru.service.CategoryService;
import com.ooru.service.ShopService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ShopService shopService;
    private final AnalyticsService analyticsService;
    private final CategoryService categoryService;

    public AdminController(ShopService shopService, AnalyticsService analyticsService, CategoryService categoryService) {
        this.shopService = shopService;
        this.analyticsService = analyticsService;
        this.categoryService = categoryService;
    }

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> analytics() {
        return ResponseEntity.ok(analyticsService.summary());
    }

    // --- Category management — this is what makes adding/editing a category a data change, not a code change ---

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> allCategories() {
        return ResponseEntity.ok(categoryService.all().stream().map(categoryService::toResponse).toList());
    }

    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest req) {
        return ResponseEntity.ok(categoryService.toResponse(categoryService.create(req)));
    }

    @PatchMapping("/categories/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable Long id, @RequestBody UpdateCategoryRequest req) {
        return ResponseEntity.ok(categoryService.toResponse(categoryService.update(id, req)));
    }

    @GetMapping("/shops/pending")
    public ResponseEntity<List<Shop>> pendingShops() {
        return ResponseEntity.ok(shopService.pendingApproval());
    }

    /** All shops, any status — this is what backs "delete" in the admin UI, which is really suspension (see ShopService). */
    @GetMapping("/shops")
    public ResponseEntity<List<Shop>> allShops() {
        return ResponseEntity.ok(shopService.all());
    }

    @PatchMapping("/shops/{shopId}/approve")
    public ResponseEntity<Shop> approve(@PathVariable Long shopId) {
        return ResponseEntity.ok(shopService.setStatus(shopId, Shop.ShopStatus.APPROVED));
    }

    @PatchMapping("/shops/{shopId}/reject")
    public ResponseEntity<Shop> reject(@PathVariable Long shopId) {
        return ResponseEntity.ok(shopService.setStatus(shopId, Shop.ShopStatus.REJECTED));
    }

    @PatchMapping("/shops/{shopId}/suspend")
    public ResponseEntity<Shop> suspend(@PathVariable Long shopId) {
        return ResponseEntity.ok(shopService.setStatus(shopId, Shop.ShopStatus.SUSPENDED));
    }
}
