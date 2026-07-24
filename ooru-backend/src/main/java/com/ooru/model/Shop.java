package com.ooru.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "shops")
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String shopName;

    // Matches the ServiceCategory.code this shop offers, e.g. "tailor", "xerox", "ac", "plumber", "electrician".
    @Column(nullable = false)
    private String categoryCode;

    @Column(nullable = false)
    private String address;

    private Double latitude;
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShopStatus status = ShopStatus.PENDING;

    // Optional, shop-owner-configurable billing extras — null/0 means "not charged".
    private Double gstPercent;
    private Long handlingFeePaise;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public enum ShopStatus { PENDING, APPROVED, REJECTED, SUSPENDED }

    public Shop() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public ShopStatus getStatus() { return status; }
    public void setStatus(ShopStatus status) { this.status = status; }
    public Double getGstPercent() { return gstPercent; }
    public void setGstPercent(Double gstPercent) { this.gstPercent = gstPercent; }
    public Long getHandlingFeePaise() { return handlingFeePaise; }
    public void setHandlingFeePaise(Long handlingFeePaise) { this.handlingFeePaise = handlingFeePaise; }
    public Instant getCreatedAt() { return createdAt; }
}
