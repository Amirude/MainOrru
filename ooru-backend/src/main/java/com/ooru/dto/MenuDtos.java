package com.ooru.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class MenuDtos {

    public static class CreateMenuItemRequest {
        @NotBlank
        public String name;
        @NotNull @Positive
        public Long pricePaise;
        public String imageUrl; // optional
    }

    public static class MenuItemResponse {
        public Long id;
        public String name;
        public Long pricePaise;
        public String imageUrl;

        public MenuItemResponse(Long id, String name, Long pricePaise, String imageUrl) {
            this.id = id;
            this.name = name;
            this.pricePaise = pricePaise;
            this.imageUrl = imageUrl;
        }
    }

    public static class OrderItemRequest {
        @NotNull
        public Long menuItemId;
        @NotNull @Positive
        public Integer quantity;
    }
}
