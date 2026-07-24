package com.ooru.repository;

import com.ooru.model.Booking;
import com.ooru.model.Review;
import com.ooru.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByShopOrderByCreatedAtDesc(Shop shop);
    boolean existsByBooking(Booking booking);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.shop = :shop")
    Double averageRatingForShop(@Param("shop") Shop shop);

    long countByShop(Shop shop);
}
