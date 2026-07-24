package com.ooru.controller;

import com.ooru.dto.BookingDtos.*;
import com.ooru.model.Booking;
import com.ooru.model.BookingStatus;
import com.ooru.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> create(Authentication auth, @Valid @RequestBody CreateBookingRequest req) {
        Long customerUserId = (Long) auth.getPrincipal();
        Booking booking = bookingService.create(customerUserId, req);
        return ResponseEntity.ok(bookingService.toResponse(booking));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<BookingResponse>> myBookings(Authentication auth) {
        Long customerUserId = (Long) auth.getPrincipal();
        List<BookingResponse> result = bookingService.myBookings(customerUserId).stream()
                .map(bookingService::toResponse).toList();
        return ResponseEntity.ok(result);
    }

    /** "Order again" — built only from this customer's own past orders, see BookingService for why. */
    @GetMapping("/mine/frequent-items")
    public ResponseEntity<List<Map<String, Object>>> frequentItems(Authentication auth) {
        Long customerUserId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(bookingService.frequentlyOrderedItems(customerUserId, 6));
    }

    /** Backs the receipt view — a single booking plus whatever payment info exists for it. */
    @GetMapping("/{bookingId}/receipt")
    public ResponseEntity<Map<String, Object>> receipt(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.receiptFor(bookingId));
    }

    /** For a shop owner viewing bookings routed to one of their shops. */
    @GetMapping("/shop/{shopId}")
    public ResponseEntity<List<BookingResponse>> shopBookings(@PathVariable Long shopId) {
        List<BookingResponse> result = bookingService.shopBookings(shopId).stream()
                .map(bookingService::toResponse).toList();
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{bookingId}/status")
    public ResponseEntity<BookingResponse> updateStatus(@PathVariable Long bookingId,
                                                         @Valid @RequestBody UpdateStatusRequest req) {
        BookingStatus status = BookingStatus.valueOf(req.status);
        Booking booking = bookingService.updateStatus(bookingId, status);
        return ResponseEntity.ok(bookingService.toResponse(booking));
    }

    @PatchMapping("/{bookingId}/assign-shop/{shopId}")
    public ResponseEntity<BookingResponse> assignShop(@PathVariable Long bookingId, @PathVariable Long shopId) {
        Booking booking = bookingService.assignShop(bookingId, shopId);
        return ResponseEntity.ok(bookingService.toResponse(booking));
    }

    /** Customer chooses pickup or delivery once the shop has marked the booking COMPLETED (ready). */
    @PatchMapping("/{bookingId}/fulfillment")
    public ResponseEntity<BookingResponse> setFulfillment(Authentication auth, @PathVariable Long bookingId,
                                                           @Valid @RequestBody FulfillmentRequest req) {
        Long customerUserId = (Long) auth.getPrincipal();
        Booking booking = bookingService.setFulfillment(customerUserId, bookingId, req);
        return ResponseEntity.ok(bookingService.toResponse(booking));
    }

    // --- Delivery partner ---

    public static class LocationUpdateRequest {
        public double lat;
        public double lng;
    }

    @GetMapping("/deliveries/available")
    public ResponseEntity<List<BookingResponse>> availableDeliveries() {
        return ResponseEntity.ok(bookingService.availableDeliveries().stream().map(bookingService::toResponse).toList());
    }

    @GetMapping("/deliveries/mine")
    public ResponseEntity<List<BookingResponse>> myDeliveries(Authentication auth) {
        Long deliveryPartnerUserId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(bookingService.myDeliveries(deliveryPartnerUserId).stream().map(bookingService::toResponse).toList());
    }

    @GetMapping("/deliveries/stats")
    public ResponseEntity<Map<String, Object>> deliveryStats(Authentication auth) {
        Long deliveryPartnerUserId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(bookingService.deliveryStats(deliveryPartnerUserId));
    }

    @PostMapping("/{bookingId}/claim-delivery")
    public ResponseEntity<BookingResponse> claimDelivery(Authentication auth, @PathVariable Long bookingId) {
        Long deliveryPartnerUserId = (Long) auth.getPrincipal();
        Booking booking = bookingService.claimDelivery(deliveryPartnerUserId, bookingId);
        return ResponseEntity.ok(bookingService.toResponse(booking));
    }

    @PatchMapping("/{bookingId}/location")
    public ResponseEntity<BookingResponse> updateLocation(Authentication auth, @PathVariable Long bookingId,
                                                           @RequestBody LocationUpdateRequest req) {
        Long deliveryPartnerUserId = (Long) auth.getPrincipal();
        Booking booking = bookingService.updateDeliveryLocation(deliveryPartnerUserId, bookingId, req.lat, req.lng);
        return ResponseEntity.ok(bookingService.toResponse(booking));
    }
}
