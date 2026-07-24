package com.ooru.dto;

import com.ooru.model.Shop;
import jakarta.validation.constraints.NotBlank;

public class ShopDtos {

    public static class RegisterShopRequest {
        @NotBlank
        public String shopName;
        @NotBlank
        public String categoryCode;
        @NotBlank
        public String address;
        public Double latitude;
        public Double longitude;
    }

    public static class ShopResponse {
        public Long id;
        public String shopName;
        public String categoryCode;
        public String address;
        public Shop.ShopStatus status;
    }

    public static class BillingSettingsRequest {
        public Double gstPercent;      // e.g. 5.0 for 5% — null or 0 means no GST charged
        public Long handlingFeePaise;  // flat fee added per order — null or 0 means none
    }

    /** Used by the nearby-shops lookup (petrol bunks, or any category, from the person's current location). */
    public static class ShopWithDistance {
        public Long id;
        public String shopName;
        public String categoryCode;
        public String address;
        public Double latitude;
        public Double longitude;
        public double distanceKm;

        public ShopWithDistance(Long id, String shopName, String categoryCode, String address,
                                 Double latitude, Double longitude, double distanceKm) {
            this.id = id;
            this.shopName = shopName;
            this.categoryCode = categoryCode;
            this.address = address;
            this.latitude = latitude;
            this.longitude = longitude;
            this.distanceKm = distanceKm;
        }
    }
}
