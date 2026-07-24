package com.ooru.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public class ComplaintDtos {

    public static class CreateComplaintRequest {
        public Long bookingId; // optional
        @NotBlank public String subject;
        @NotBlank public String description;
    }

    public static class RespondRequest {
        @NotBlank public String status; // "OPEN", "IN_PROGRESS", "RESOLVED"
        public String adminResponse;
    }

    public static class ComplaintResponse {
        public Long id;
        public String raisedByName;
        public Long bookingId;
        public String bookingReference;
        public String subject;
        public String description;
        public String status;
        public String adminResponse;
        public Instant createdAt;
        public Instant resolvedAt;
    }
}
