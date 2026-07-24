package com.ooru.repository;

import com.ooru.model.AppointmentSlot;
import com.ooru.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentSlotRepository extends JpaRepository<AppointmentSlot, Long> {
    List<AppointmentSlot> findByShopAndBookedFalseAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(Shop shop, LocalDate fromDate);
    List<AppointmentSlot> findByShop(Shop shop);
}
