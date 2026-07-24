package com.ooru.model;

import jakarta.persistence.*;

/**
 * Only used by shops in the "food" and "grocery" categories. Prices are stored in paise and are
 * always read from here at order time — a booking's total is computed server-side from these
 * rows, never trusted from whatever the frontend cart sent.
 */
@Entity
@Table(name = "menu_items")
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Long pricePaise;

    // A plain image URL rather than file upload/storage — keeps this backend free of needing an
    // S3-style file store. Shop owners host the image themselves (or use any free image host)
    // and paste the link in.
    private String imageUrl;

    @Column(nullable = false)
    private boolean active = true;

    public MenuItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Shop getShop() { return shop; }
    public void setShop(Shop shop) { this.shop = shop; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getPricePaise() { return pricePaise; }
    public void setPricePaise(Long pricePaise) { this.pricePaise = pricePaise; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
