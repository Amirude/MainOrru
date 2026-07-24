package com.ooru.dto;

import com.ooru.model.BookingStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class BookingDtos {

    public static class CreateBookingRequest {
        @NotBlank
        public String categoryCode; // "tailor", "xerox", "ac", "plumber", "electrician", "food", "grocery", "parcel", "rental", "driver"

        // The category-specific fields, e.g. {"garment":"Shirt","address":"...","date":"2026-07-20"}.
        // For "food" / "grocery" this typically just holds "address" and any delivery notes —
        // the actual order contents go in `items` below so pricing is computed server-side.
        @NotNull
        public Map<String, String> details;

        // Only used for "food" and "grocery" categories. Requires shopId to also be set, since
        // items are looked up against that shop's own menu.
        public List<MenuDtos.OrderItemRequest> items;

        // Optional — if the customer picked a specific shop from the list. If omitted, an admin/dispatcher assigns one.
        public Long shopId;

        // Optional — the customer's live coordinates at checkout, used to compute a REAL
        // distance-based delivery fee (see BookingService). Falls back to a flat estimate if
        // omitted (e.g. the browser denied location permission).
        public Double customerLat;
        public Double customerLng;

        // "ONLINE" (default, goes through Razorpay) or "COD" (cash on delivery — no gateway call).
        public String paymentMethod;

        // Optional — for categories using real appointment slots (currently: tailor). Must belong
        // to shopId. See SlotService.claim for what happens if two people pick the same slot.
        public Long slotId;
    }

    public static class UpdateStatusRequest {
        @NotBlank
        public String status; // must match one of BookingStatus's values
    }

    public static class FulfillmentRequest {
        @NotBlank
        public String method; // "PICKUP" or "DELIVERY"
        public String address; // required when method is "DELIVERY"
    }

    public static class BookingResponse {
        public Long id;
        public String reference;
        public String categoryCode;
        public Map<String, Object> details; // may include nested "items" list and "totalPaise" for food/grocery orders
        public BookingStatus status;
        public Long shopId;
        public String shopName;
        public boolean hasReview;
        public String customerPhone;
        public String deliveryPartnerName;
        public Double deliveryLat;
        public Double deliveryLng;
        public Instant createdAt;
        public Instant updatedAt;
    }
}
