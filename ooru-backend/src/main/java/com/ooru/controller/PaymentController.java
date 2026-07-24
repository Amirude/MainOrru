package com.ooru.controller;

import com.ooru.model.Booking;
import com.ooru.model.Payment;
import com.ooru.repository.BookingRepository;
import com.ooru.service.PaymentService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final BookingRepository bookingRepository;

    public PaymentController(PaymentService paymentService, BookingRepository bookingRepository) {
        this.paymentService = paymentService;
        this.bookingRepository = bookingRepository;
    }

    public static class CreateOrderRequest {
        @NotNull public Long bookingId;
        @NotNull public Long amountPaise; // amount in paise, e.g. 49900 = ₹499.00
    }

    public static class VerifyRequest {
        @NotBlank public String razorpayOrderId;
        @NotBlank public String razorpayPaymentId;
        @NotBlank public String razorpaySignature;
    }

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest req) throws Exception {
        Booking booking = bookingRepository.findById(req.bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        Payment payment = paymentService.createOrder(booking, req.amountPaise);
        // The frontend uses razorpayOrderId + your public key id to open Razorpay's Checkout widget.
        return ResponseEntity.ok(Map.of(
                "razorpayOrderId", payment.getRazorpayOrderId(),
                "amountPaise", payment.getAmountPaise(),
                "currency", payment.getCurrency()
        ));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody VerifyRequest req) throws Exception {
        boolean valid = paymentService.verifyAndMarkPaid(req.razorpayOrderId, req.razorpayPaymentId, req.razorpaySignature);
        if (!valid) {
            return ResponseEntity.badRequest().body(Map.of("message", "Payment signature could not be verified"));
        }
        return ResponseEntity.ok(Map.of("message", "Payment verified"));
    }

    /** No gateway involved — just records that cash is expected for this booking. */
    @PostMapping("/cod")
    public ResponseEntity<?> createCod(@RequestBody CreateOrderRequest req) {
        Booking booking = bookingRepository.findById(req.bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        Payment payment = paymentService.createCodOrder(booking, req.amountPaise);
        return ResponseEntity.ok(Map.of("message", "Cash on delivery recorded", "amountPaise", payment.getAmountPaise()));
    }

    /** The shop or delivery partner calls this once cash is actually in hand. */
    @PatchMapping("/cod/{bookingId}/collected")
    public ResponseEntity<?> markCollected(@PathVariable Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        paymentService.markCodCollected(booking);
        return ResponseEntity.ok(Map.of("message", "Marked as collected"));
    }
}
