package com.ooru.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class CategoryDtos {

    public static class FieldDefDto {
        @NotBlank public String id;
        @NotBlank public String label;
        @NotBlank public String type; // "text", "textarea", "select", "date", "time"
        public List<String> options; // required when type is "select"
    }

    public static class CreateCategoryRequest {
        @NotBlank public String code;
        @NotBlank public String displayName;
        public String icon;
        @NotNull public List<FieldDefDto> fields;
    }

    public static class UpdateCategoryRequest {
        public String displayName;
        public String icon;
        public List<FieldDefDto> fields;
        public Boolean active;
    }

    public static class CategoryResponse {
        public Long id;
        public String code;
        public String displayName;
        public String icon;
        public boolean active;
        public List<FieldDefDto> fields; // empty for categories with a dedicated page (tailor, food, grocery)
    }
}
