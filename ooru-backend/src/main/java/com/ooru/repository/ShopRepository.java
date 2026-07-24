package com.ooru.repository;

import com.ooru.model.Shop;
import com.ooru.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ShopRepository extends JpaRepository<Shop, Long> {
    List<Shop> findByStatus(Shop.ShopStatus status);
    List<Shop> findByOwner(User owner);
    List<Shop> findByCategoryCodeAndStatus(String categoryCode, Shop.ShopStatus status);

    @org.springframework.data.jpa.repository.Query(
        "SELECT s FROM Shop s WHERE s.status = 'APPROVED' AND " +
        "(LOWER(s.shopName) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(s.address) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Shop> searchApproved(@org.springframework.data.repository.query.Param("q") String q);
}
