package com.ooru.repository;

import com.ooru.model.MenuItem;
import com.ooru.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByShopAndActiveTrue(Shop shop);
    List<MenuItem> findByShop(Shop shop);

    @org.springframework.data.jpa.repository.Query(
        "SELECT m FROM MenuItem m WHERE m.active = true AND m.shop.status = 'APPROVED' AND " +
        "LOWER(m.name) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<MenuItem> searchActive(@org.springframework.data.repository.query.Param("q") String q);
}
