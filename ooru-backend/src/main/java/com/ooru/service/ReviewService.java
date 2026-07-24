package com.ooru.service;

import com.ooru.dto.ReviewDtos.*;
import com.ooru.model.Booking;
import com.ooru.model.BookingStatus;
import com.ooru.model.Review;
import com.ooru.model.Shop;
import com.ooru.repository.BookingRepository;
import com.ooru.repository.ReviewRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;

    public ReviewService(ReviewRepository reviewRepository, BookingRepository bookingRepository) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
    }

    public Review submit(Long customerUserId, CreateReviewRequest req) {
        Booking booking = bookingRepository.findById(req.bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (!booking.getCustomer().getId().equals(customerUserId)) {
            throw new IllegalStateException("This isn't your booking");
        }
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalStateException("You can only review a completed booking");
        }
        if (booking.getShop() == null) {
            throw new IllegalStateException("This booking has no shop to review");
        }
        if (reviewRepository.existsByBooking(booking)) {
            throw new IllegalStateException("You've already reviewed this booking");
        }

        Review review = new Review();
        review.setBooking(booking);
        review.setShop(booking.getShop());
        review.setCustomer(booking.getCustomer());
        review.setRating(req.rating);
        review.setComment(req.comment);
        if (booking.getDeliveryPartner() != null && req.deliveryPartnerRating != null) {
            review.setDeliveryPartner(booking.getDeliveryPartner());
            review.setDeliveryPartnerRating(req.deliveryPartnerRating);
        }
        return reviewRepository.save(review);
    }

    public ShopReviewsResponse forShop(Shop shop) {
        List<Review> reviews = reviewRepository.findByShopOrderByCreatedAtDesc(shop);
        ShopReviewsResponse res = new ShopReviewsResponse();
        res.averageRating = reviewRepository.averageRatingForShop(shop);
        res.reviewCount = reviewRepository.countByShop(shop);
        res.reviews = reviews.stream().map(r -> {
            ReviewResponse rr = new ReviewResponse();
            rr.id = r.getId();
            rr.customerName = r.getCustomer().getName();
            rr.rating = r.getRating();
            rr.deliveryPartnerRating = r.getDeliveryPartnerRating();
            rr.comment = r.getComment();
            rr.createdAt = r.getCreatedAt();
            return rr;
        }).toList();
        return res;
    }
}
