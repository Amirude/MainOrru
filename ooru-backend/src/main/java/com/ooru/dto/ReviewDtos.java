package com.ooru.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public class ReviewDtos {

    public static class CreateReviewRequest {
        @NotNull
        public Long bookingId;
        @Min(1) @Max(5)
        public int rating; // for the shop
        @Min(1) @Max(5)
        public Integer deliveryPartnerRating; // optional — only meaningful if this booking had a delivery
        public String comment;
    }

    public static class ReviewResponse {
        public Long id;
        public String customerName;
        public int rating;
        public Integer deliveryPartnerRating;
        public String comment;
        public Instant createdAt;
    }

    public static class ShopReviewsResponse {
        public Double averageRating; // null if no reviews yet
        public long reviewCount;
        public java.util.List<ReviewResponse> reviews;
    }
}
