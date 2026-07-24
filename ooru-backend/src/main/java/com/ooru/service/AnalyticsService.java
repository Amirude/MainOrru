package com.ooru.service;

import com.ooru.model.Payment;
import com.ooru.repository.*;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every number here is a real COUNT/SUM against the database — no model, no prediction, nothing
 * dressed up as more than it is. This is what "analytics" honestly looks like before there's
 * enough real traffic for anything more sophisticated to mean something.
 */
@Service
public class AnalyticsService {

    private final BookingRepository bookingRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final ServiceCategoryRepository categoryRepository;

    public AnalyticsService(BookingRepository bookingRepository, ShopRepository shopRepository,
                             UserRepository userRepository, PaymentRepository paymentRepository,
                             ServiceCategoryRepository categoryRepository) {
        this.bookingRepository = bookingRepository;
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.categoryRepository = categoryRepository;
    }

    public Map<String, Object> summary() {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("totalUsers", userRepository.count());
        result.put("totalShops", shopRepository.count());
        result.put("shopsByStatus", countBy(shopRepository.findAll(), s -> s.getStatus().name()));

        var allBookings = bookingRepository.findAll();
        result.put("totalBookings", (long) allBookings.size());
        result.put("bookingsByStatus", countBy(allBookings, b -> b.getStatus().name()));
        result.put("bookingsByCategory", countBy(allBookings, b -> b.getCategoryCode()));

        long paidCount = 0;
        long totalRevenuePaise = 0;
        for (Payment p : paymentRepository.findAll()) {
            if (p.getStatus() == Payment.PaymentStatus.PAID) {
                paidCount++;
                totalRevenuePaise += p.getAmountPaise();
            }
        }
        result.put("paidTransactionCount", paidCount);
        result.put("totalRevenuePaise", totalRevenuePaise);

        result.put("activeCategoryCount", categoryRepository.findByActiveTrue().size());

        return result;
    }

    private <T> Map<String, Long> countBy(List<T> items, java.util.function.Function<T, String> keyFn) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (T item : items) {
            counts.merge(keyFn.apply(item), 1L, Long::sum);
        }
        return counts;
    }
}
