package com.ooru.controller;

import com.ooru.dto.MenuDtos.CreateMenuItemRequest;
import com.ooru.dto.MenuDtos.MenuItemResponse;
import com.ooru.dto.ShopDtos.*;
import com.ooru.dto.SlotDtos.CreateSlotRequest;
import com.ooru.dto.SlotDtos.SlotResponse;
import com.ooru.model.AppointmentSlot;
import com.ooru.model.MenuItem;
import com.ooru.model.Shop;
import com.ooru.service.MenuService;
import com.ooru.service.ShopService;
import com.ooru.service.SlotService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shops")
public class ShopController {

    private final ShopService shopService;
    private final MenuService menuService;
    private final SlotService slotService;

    public ShopController(ShopService shopService, MenuService menuService, SlotService slotService) {
        this.shopService = shopService;
        this.menuService = menuService;
        this.slotService = slotService;
    }

    @PostMapping("/register")
    public ResponseEntity<Shop> register(Authentication auth, @Valid @RequestBody RegisterShopRequest req) {
        Long ownerUserId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(shopService.register(ownerUserId, req));
    }

    /** Shop owner sets their own GST% and flat handling fee — applied to every bill from this shop. */
    @PatchMapping("/{shopId}/billing-settings")
    public ResponseEntity<Shop> billingSettings(Authentication auth, @PathVariable Long shopId,
                                                 @RequestBody BillingSettingsRequest req) {
        Long ownerUserId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(shopService.updateBillingSettings(ownerUserId, shopId, req));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<Shop>> myShops(Authentication auth) {
        Long ownerUserId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(shopService.myShops(ownerUserId));
    }

    /** Public — lets a customer see which approved shops offer a given category before booking. */
    @GetMapping("/by-category/{categoryCode}")
    public ResponseEntity<List<Shop>> byCategory(@PathVariable String categoryCode) {
        return ResponseEntity.ok(shopService.approvedByCategory(categoryCode));
    }

    /** Public — e.g. GET /api/shops/nearby?categoryCode=petrol&lat=13.08&lng=80.27 */
    @GetMapping("/nearby")
    public ResponseEntity<List<ShopWithDistance>> nearby(@RequestParam String categoryCode,
                                                          @RequestParam double lat,
                                                          @RequestParam double lng) {
        return ResponseEntity.ok(shopService.nearby(categoryCode, lat, lng));
    }

    // --- Menu (food / grocery shops only, but harmless to expose generally) ---

    @PostMapping("/{shopId}/menu")
    public ResponseEntity<MenuItemResponse> addMenuItem(Authentication auth, @PathVariable Long shopId,
                                                          @Valid @RequestBody CreateMenuItemRequest req) {
        Long ownerUserId = (Long) auth.getPrincipal();
        MenuItem item = menuService.addItem(ownerUserId, shopId, req);
        return ResponseEntity.ok(new MenuItemResponse(item.getId(), item.getName(), item.getPricePaise(), item.getImageUrl()));
    }

    /** Public — a customer needs to see the menu before adding items to a cart. */
    @GetMapping("/{shopId}/menu")
    public ResponseEntity<List<MenuItemResponse>> getMenu(@PathVariable Long shopId) {
        List<MenuItemResponse> menu = menuService.activeMenuFor(shopId).stream()
                .map(i -> new MenuItemResponse(i.getId(), i.getName(), i.getPricePaise(), i.getImageUrl()))
                .toList();
        return ResponseEntity.ok(menu);
    }

    // --- Appointment slots (tailor, or any category needing real scheduling) ---

    @PostMapping("/{shopId}/slots")
    public ResponseEntity<SlotResponse> createSlot(Authentication auth, @PathVariable Long shopId,
                                                    @Valid @RequestBody CreateSlotRequest req) {
        Long ownerUserId = (Long) auth.getPrincipal();
        AppointmentSlot slot = slotService.createSlot(ownerUserId, shopId, req);
        return ResponseEntity.ok(new SlotResponse(slot.getId(), slot.getDate(), slot.getStartTime(), slot.getEndTime()));
    }

    /** Public — only ever returns slots that are still open, so two people never see a taken one as available. */
    @GetMapping("/{shopId}/slots")
    public ResponseEntity<List<SlotResponse>> getAvailableSlots(@PathVariable Long shopId) {
        List<SlotResponse> slots = slotService.availableSlots(shopId).stream()
                .map(s -> new SlotResponse(s.getId(), s.getDate(), s.getStartTime(), s.getEndTime()))
                .toList();
        return ResponseEntity.ok(slots);
    }
}
