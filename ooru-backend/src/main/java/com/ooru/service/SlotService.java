package com.ooru.service;

import com.ooru.dto.SlotDtos.CreateSlotRequest;
import com.ooru.model.AppointmentSlot;
import com.ooru.model.Shop;
import com.ooru.repository.AppointmentSlotRepository;
import com.ooru.repository.ShopRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class SlotService {

    private final AppointmentSlotRepository slotRepository;
    private final ShopRepository shopRepository;

    public SlotService(AppointmentSlotRepository slotRepository, ShopRepository shopRepository) {
        this.slotRepository = slotRepository;
        this.shopRepository = shopRepository;
    }

    public AppointmentSlot createSlot(Long ownerUserId, Long shopId, CreateSlotRequest req) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("Shop not found"));
        if (!shop.getOwner().getId().equals(ownerUserId)) {
            throw new IllegalStateException("You don't own this shop");
        }
        if (!req.endTime.isAfter(req.startTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        AppointmentSlot slot = new AppointmentSlot();
        slot.setShop(shop);
        slot.setDate(req.date);
        slot.setStartTime(req.startTime);
        slot.setEndTime(req.endTime);
        return slotRepository.save(slot);
    }

    /** Public — only ever shows slots that are still open, from today onward. */
    public List<AppointmentSlot> availableSlots(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("Shop not found"));
        return slotRepository.findByShopAndBookedFalseAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(shop, LocalDate.now());
    }

    /**
     * Atomically claims a slot for a booking. Callers MUST treat an IllegalStateException here as
     * "someone else just took it" and tell the customer to pick another slot — this is exactly the
     * kind of double-booking race a real appointment system has to guard against.
     */
    public AppointmentSlot claim(Long slotId, Long shopId) {
        AppointmentSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found"));
        if (!slot.getShop().getId().equals(shopId)) {
            throw new IllegalArgumentException("Slot does not belong to the selected shop");
        }
        if (slot.isBooked()) {
            throw new IllegalStateException("That slot was just taken — please pick another one");
        }
        slot.setBooked(true);
        try {
            return slotRepository.saveAndFlush(slot);
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            // Two people tried to claim the same slot at the same instant — @Version on the
            // entity catches this at the database level, which the isBooked() check above alone
            // cannot (that check and this write aren't atomic without it).
            throw new IllegalStateException("That slot was just taken — please pick another one");
        }
    }
}
