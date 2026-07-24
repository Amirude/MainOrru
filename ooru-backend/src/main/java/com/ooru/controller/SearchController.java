package com.ooru.controller;

import com.ooru.model.MenuItem;
import com.ooru.model.Shop;
import com.ooru.repository.MenuItemRepository;
import com.ooru.repository.ShopRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class SearchController {

    private final ShopRepository shopRepository;
    private final MenuItemRepository menuItemRepository;

    public SearchController(ShopRepository shopRepository, MenuItemRepository menuItemRepository) {
        this.shopRepository = shopRepository;
        this.menuItemRepository = menuItemRepository;
    }

    public record ShopResult(Long id, String shopName, String categoryCode, String address) {}
    public record MenuItemResult(Long id, String name, Long pricePaise, String imageUrl, Long shopId, String shopName) {}

    /** e.g. GET /api/search?q=tailor or ?q=dosa — plain substring search, no external service needed. */
    @GetMapping("/api/search")
    public ResponseEntity<Map<String, Object>> search(@RequestParam String q) {
        if (q == null || q.isBlank()) {
            return ResponseEntity.ok(Map.of("shops", List.of(), "menuItems", List.of()));
        }
        List<ShopResult> shops = shopRepository.searchApproved(q).stream()
                .map(s -> new ShopResult(s.getId(), s.getShopName(), s.getCategoryCode(), s.getAddress()))
                .limit(10)
                .toList();
        List<MenuItemResult> menuItems = menuItemRepository.searchActive(q).stream()
                .map(m -> new MenuItemResult(m.getId(), m.getName(), m.getPricePaise(), m.getImageUrl(),
                        m.getShop().getId(), m.getShop().getShopName()))
                .limit(10)
                .toList();
        return ResponseEntity.ok(Map.of("shops", shops, "menuItems", menuItems));
    }
}
