package com.ooru.controller;

import com.ooru.dto.ReviewDtos.*;
import com.ooru.model.Review;
import com.ooru.model.Shop;
import com.ooru.repository.ShopRepository;
import com.ooru.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class ReviewController {

    private final ReviewService reviewService;
    private final ShopRepository shopRepository;

    public ReviewController(ReviewService reviewService, ShopRepository shopRepository) {
        this.reviewService = reviewService;
        this.shopRepository = shopRepository;
    }

    @PostMapping("/api/reviews")
    public ResponseEntity<?> submit(Authentication auth, @Valid @RequestBody CreateReviewRequest req) {
        Long customerUserId = (Long) auth.getPrincipal();
        Review review = reviewService.submit(customerUserId, req);
        return ResponseEntity.ok(java.util.Map.of("id", review.getId(), "message", "Review submitted"));
    }

    /** Public — reviews build trust before a customer has even booked. */
    @GetMapping("/api/shops/{shopId}/reviews")
    public ResponseEntity<ShopReviewsResponse> forShop(@PathVariable Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("Shop not found"));
        return ResponseEntity.ok(reviewService.forShop(shop));
    }
}
