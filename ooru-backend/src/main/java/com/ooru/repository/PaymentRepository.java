package com.ooru.repository;

import com.ooru.model.Booking;
import com.ooru.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByBooking(Booking booking);
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
}
