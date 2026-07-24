package com.ooru.repository;

import com.ooru.model.Booking;
import com.ooru.model.User;
import com.ooru.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByCustomerOrderByCreatedAtDesc(User customer);
    List<Booking> findByShopOrderByCreatedAtDesc(Shop shop);
    Optional<Booking> findByReference(String reference);
    List<Booking> findByDeliveryPartnerIsNullAndCategoryCodeInOrderByCreatedAtDesc(List<String> categoryCodes);
    List<Booking> findByDeliveryPartnerOrderByCreatedAtDesc(User deliveryPartner);
}
