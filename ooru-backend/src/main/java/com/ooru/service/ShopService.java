package com.ooru.service;

import com.ooru.dto.ShopDtos.*;
import com.ooru.model.Shop;
import com.ooru.model.User;
import com.ooru.repository.ShopRepository;
import com.ooru.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class ShopService {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;

    public ShopService(ShopRepository shopRepository, UserRepository userRepository) {
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
    }

    public Shop register(Long ownerUserId, RegisterShopRequest req) {
        User owner = userRepository.findById(ownerUserId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        Shop shop = new Shop();
        shop.setOwner(owner);
        shop.setShopName(req.shopName);
        shop.setCategoryCode(req.categoryCode);
        shop.setAddress(req.address);
        shop.setLatitude(req.latitude);
        shop.setLongitude(req.longitude);
        shop.setStatus(Shop.ShopStatus.PENDING);
        return shopRepository.save(shop);
    }

    public List<Shop> myShops(Long ownerUserId) {
        User owner = userRepository.findById(ownerUserId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        return shopRepository.findByOwner(owner);
    }

    public List<Shop> pendingApproval() {
        return shopRepository.findByStatus(Shop.ShopStatus.PENDING);
    }

    public List<Shop> all() {
        return shopRepository.findAll();
    }

    public Shop setStatus(Long shopId, Shop.ShopStatus status) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalStateException("Shop not found"));
        shop.setStatus(status);
        return shopRepository.save(shop);
    }

    public List<Shop> approvedByCategory(String categoryCode) {
        return shopRepository.findByCategoryCodeAndStatus(categoryCode, Shop.ShopStatus.APPROVED);
    }

    public Shop updateBillingSettings(Long ownerUserId, Long shopId, BillingSettingsRequest req) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("Shop not found"));
        if (!shop.getOwner().getId().equals(ownerUserId)) {
            throw new IllegalStateException("You don't own this shop");
        }
        shop.setGstPercent(req.gstPercent);
        shop.setHandlingFeePaise(req.handlingFeePaise);
        return shopRepository.save(shop);
    }

    /**
     * Distance is computed here on the backend rather than trusting a client-side calculation,
     * so "nearest first" is consistent no matter which frontend calls this.
     */
    public List<ShopWithDistance> nearby(String categoryCode, double lat, double lng) {
        return shopRepository.findByCategoryCodeAndStatus(categoryCode, Shop.ShopStatus.APPROVED).stream()
                .filter(s -> s.getLatitude() != null && s.getLongitude() != null)
                .map(s -> new ShopWithDistance(
                        s.getId(), s.getShopName(), s.getCategoryCode(), s.getAddress(),
                        s.getLatitude(), s.getLongitude(),
                        haversineKm(lat, lng, s.getLatitude(), s.getLongitude())))
                .sorted(Comparator.comparingDouble(sd -> sd.distanceKm))
                .toList();
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
