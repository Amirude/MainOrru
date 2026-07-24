package com.ooru.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.ooru.model.Booking;
import com.ooru.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    /**
     * Fire-and-forget: notification delivery failing should never break a booking status update.
     * Safe to call even when Firebase isn't configured — it just logs and returns.
     */
    public void notifyBookingStatusChanged(Booking booking) {
        User customer = booking.getCustomer();
        if (FirebaseApp.getApps().isEmpty()) {
            log.debug("Firebase not configured — skipping notification for booking {}", booking.getReference());
            return;
        }
        if (customer.getFcmToken() == null || customer.getFcmToken().isBlank()) {
            log.debug("Customer {} has no FCM token registered — skipping notification", customer.getId());
            return;
        }

        String title = "Altenul One — " + booking.getReference();
        String body = switch (booking.getStatus()) {
            case ACCEPTED -> "Your " + booking.getCategoryCode() + " booking was accepted.";
            case IN_PROGRESS -> "Work has started on your " + booking.getCategoryCode() + " booking.";
            case COMPLETED -> "tailor".equals(booking.getCategoryCode())
                    ? "Your clothes are ready! Open the app to choose pickup or delivery."
                    : "Your " + booking.getCategoryCode() + " booking is complete.";
            case REJECTED -> "Your " + booking.getCategoryCode() + " booking was declined.";
            case CANCELLED -> "Your " + booking.getCategoryCode() + " booking was cancelled.";
            default -> "Your " + booking.getCategoryCode() + " booking status changed to " + booking.getStatus() + ".";
        };

        Message message = Message.builder()
                .setToken(customer.getFcmToken())
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .build();

        try {
            FirebaseMessaging.getInstance().send(message);
        } catch (Exception e) {
            // A bad/expired token shouldn't be treated as a server error — just log it.
            log.warn("Push notification failed for booking {}: {}", booking.getReference(), e.getMessage());
        }
    }

    /** Fired once, right when a booking lands on a shop — separate from the status-change notifications above. */
    public void notifyShopOfNewBooking(Booking booking) {
        if (booking.getShop() == null) return;
        User owner = booking.getShop().getOwner();
        if (FirebaseApp.getApps().isEmpty() || owner.getFcmToken() == null || owner.getFcmToken().isBlank()) {
            log.debug("Skipping new-booking notification for shop owner {} — Firebase not configured or no token", owner.getId());
            return;
        }
        Message message = Message.builder()
                .setToken(owner.getFcmToken())
                .setNotification(Notification.builder()
                        .setTitle("New order — " + booking.getReference())
                        .setBody("A new " + booking.getCategoryCode() + " booking just came in.")
                        .build())
                .build();
        try {
            FirebaseMessaging.getInstance().send(message);
        } catch (Exception e) {
            log.warn("New-booking notification failed for shop owner {}: {}", owner.getId(), e.getMessage());
        }
    }
}
