package com.ooru.service;

import com.ooru.model.Booking;
import com.ooru.model.BookingStatus;
import com.ooru.model.Shop;
import com.ooru.repository.BookingRepository;
import com.ooru.repository.ShopRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Real dynamic pricing: a multiplier computed from actual current data — how many bookings are
 * active right now for a category, divided by how many approved shops can serve them. No machine
 * learning, no hidden model — every number that goes into this is one you could recompute
 * yourself with a SQL query, and the formula is public (see below), not a black box.
 */
@Service
public class PricingService {

    private final BookingRepository bookingRepository;
    private final ShopRepository shopRepository;

    private static final Set<BookingStatus> ACTIVE_STATUSES =
            Set.of(BookingStatus.REQUESTED, BookingStatus.ACCEPTED, BookingStatus.IN_PROGRESS);
    private static final Duration DEMAND_WINDOW = Duration.ofMinutes(15);
    private static final double MAX_MULTIPLIER = 2.0;
    private static final double SENSITIVITY = 0.25; // how sharply demand/supply imbalance raises price

    public PricingService(BookingRepository bookingRepository, ShopRepository shopRepository) {
        this.bookingRepository = bookingRepository;
        this.shopRepository = shopRepository;
    }

    public record SurgeResult(double multiplier, long activeBookings, long approvedShops, String explanation) {}

    public SurgeResult surgeFor(String categoryCode) {
        Instant since = Instant.now().minus(DEMAND_WINDOW);

        long activeBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getCategoryCode().equals(categoryCode))
                .filter(b -> b.getCreatedAt().isAfter(since))
                .filter(b -> ACTIVE_STATUSES.contains(b.getStatus()))
                .count();

        long approvedShops = shopRepository.findByCategoryCodeAndStatus(categoryCode, Shop.ShopStatus.APPROVED).size();

        double ratio = activeBookings / (double) Math.max(1, approvedShops);
        double raw = 1.0 + Math.min(MAX_MULTIPLIER - 1.0, ratio * SENSITIVITY);
        double multiplier = Math.round(raw * 10.0) / 10.0; // round to nearest 0.1

        String explanation = String.format(
                "%d active booking(s) in the last %d minutes across %d approved shop(s) → %.1fx",
                activeBookings, DEMAND_WINDOW.toMinutes(), approvedShops, multiplier);

        return new SurgeResult(multiplier, activeBookings, approvedShops, explanation);
    }

    public long applySurge(long basePaise, double multiplier) {
        return Math.round(basePaise * multiplier);
    }
}
