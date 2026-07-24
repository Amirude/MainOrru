package com.ooru.service;

import com.ooru.dto.MenuDtos.CreateMenuItemRequest;
import com.ooru.model.MenuItem;
import com.ooru.model.Shop;
import com.ooru.repository.MenuItemRepository;
import com.ooru.repository.ShopRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MenuService {

    private final MenuItemRepository menuItemRepository;
    private final ShopRepository shopRepository;

    public MenuService(MenuItemRepository menuItemRepository, ShopRepository shopRepository) {
        this.menuItemRepository = menuItemRepository;
        this.shopRepository = shopRepository;
    }

    public MenuItem addItem(Long ownerUserId, Long shopId, CreateMenuItemRequest req) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("Shop not found"));
        if (!shop.getOwner().getId().equals(ownerUserId)) {
            throw new IllegalStateException("You don't own this shop");
        }

        MenuItem item = new MenuItem();
        item.setShop(shop);
        item.setName(req.name);
        item.setPricePaise(req.pricePaise);
        item.setImageUrl(req.imageUrl);
        return menuItemRepository.save(item);
    }

    public List<MenuItem> activeMenuFor(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("Shop not found"));
        return menuItemRepository.findByShopAndActiveTrue(shop);
    }

    public MenuItem setActive(Long ownerUserId, Long menuItemId, boolean active) {
        MenuItem item = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new IllegalArgumentException("Menu item not found"));
        if (!item.getShop().getOwner().getId().equals(ownerUserId)) {
            throw new IllegalStateException("You don't own this shop");
        }
        item.setActive(active);
        return menuItemRepository.save(item);
    }
}
