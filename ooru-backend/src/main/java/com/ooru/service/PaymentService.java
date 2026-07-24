package com.ooru.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ooru.model.Booking;
import com.ooru.model.Payment;
import com.ooru.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

/**
 * Talks to Razorpay's REST API directly over HTTPS rather than pulling in their SDK, so the only
 * extra thing you need to supply is a real key id / key secret from your Razorpay dashboard
 * (see application.yml). Sign up and complete business KYC at https://razorpay.com first —
 * that verification step is something only you can do.
 */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final String keyId;
    private final String keySecret;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PaymentService(PaymentRepository paymentRepository,
                           @Value("${ooru.razorpay.key-id}") String keyId,
                           @Value("${ooru.razorpay.key-secret}") String keySecret) {
        this.paymentRepository = paymentRepository;
        this.keyId = keyId;
        this.keySecret = keySecret;
    }

    /** Creates a Razorpay order and a local Payment record tracking it. Call this before showing the checkout UI. */
    public Payment createOrder(Booking booking, long amountPaise) throws Exception {
        String basicAuth = Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));

        String body = objectMapper.writeValueAsString(Map.of(
                "amount", amountPaise,
                "currency", "INR",
                "receipt", booking.getReference()
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.razorpay.com/v1/orders"))
                .header("Authorization", "Basic " + basicAuth)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IllegalStateException("Razorpay order creation failed: " + response.body());
        }

        JsonNode json = objectMapper.readTree(response.body());

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmountPaise(amountPaise);
        payment.setCurrency("INR");
        payment.setRazorpayOrderId(json.get("id").asText());
        payment.setStatus(Payment.PaymentStatus.CREATED);
        return paymentRepository.save(payment);
    }

    /** Cash on delivery — no gateway involved at all, just a record that cash is expected. */
    public Payment createCodOrder(Booking booking, long amountPaise) {
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmountPaise(amountPaise);
        payment.setCurrency("INR");
        payment.setMethod(Payment.PaymentMethod.COD);
        payment.setStatus(Payment.PaymentStatus.COD_PENDING);
        return paymentRepository.save(payment);
    }

    /** The shop or delivery partner marks cash actually received — only then does it count as paid. */
    public Payment markCodCollected(Booking booking) {
        Payment payment = paymentRepository.findByBooking(booking)
                .orElseThrow(() -> new IllegalStateException("No COD payment found for this booking"));
        if (payment.getMethod() != Payment.PaymentMethod.COD) {
            throw new IllegalStateException("This booking wasn't paid by cash on delivery");
        }
        payment.setStatus(Payment.PaymentStatus.PAID);
        return paymentRepository.save(payment);
    }

    /**
     * Call this from the endpoint your frontend hits after Razorpay's checkout widget succeeds.
     * NEVER mark a payment as paid just because the frontend says so — always verify the signature
     * server-side, as done here, since the frontend is not a trusted source of truth for money.
     */
    public boolean verifyAndMarkPaid(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) throws Exception {
        String payload = razorpayOrderId + "|" + razorpayPaymentId;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        String expectedSignature = HexFormat.of().formatHex(hash);

        boolean valid = expectedSignature.equals(razorpaySignature);
        if (valid) {
            Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                    .orElseThrow(() -> new IllegalStateException("No matching payment for order " + razorpayOrderId));
            payment.setRazorpayPaymentId(razorpayPaymentId);
            payment.setStatus(Payment.PaymentStatus.PAID);
            paymentRepository.save(payment);
        }
        return valid;
    }
}
