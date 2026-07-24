package com.ooru.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Human-friendly reference shown to the customer, e.g. "OOR-7F3K9".
    @Column(nullable = false, unique = true)
    private String reference;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_user_id", nullable = false)
    private User customer;

    @ManyToOne
    @JoinColumn(name = "shop_id")
    private Shop shop; // null until the platform / admin assigns a shop, or the customer picked one directly

    @ManyToOne
    @JoinColumn(name = "slot_id")
    private AppointmentSlot slot; // only set for categories using real appointment slots (currently: tailor)

    @ManyToOne
    @JoinColumn(name = "delivery_partner_id")
    private User deliveryPartner; // set once a delivery partner claims a food/grocery/parcel delivery

    private Double deliveryLat;
    private Double deliveryLng;

    @Column(nullable = false)
    private String categoryCode;

    // Free-form JSON blob holding the category-specific fields (garment type, issue, address, date, time, etc.)
    // Kept as TEXT rather than a rigid column-per-field, since Phase 2/3 add many more categories with different
    // fields. The service layer serializes/deserializes this with Jackson's ObjectMapper.
    @Lob
    @Column(nullable = false)
    private String detailsJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.REQUESTED;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    public Booking() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public User getCustomer() { return customer; }
    public void setCustomer(User customer) { this.customer = customer; }
    public Shop getShop() { return shop; }
    public void setShop(Shop shop) { this.shop = shop; }
    public AppointmentSlot getSlot() { return slot; }
    public void setSlot(AppointmentSlot slot) { this.slot = slot; }
    public User getDeliveryPartner() { return deliveryPartner; }
    public void setDeliveryPartner(User deliveryPartner) { this.deliveryPartner = deliveryPartner; }
    public Double getDeliveryLat() { return deliveryLat; }
    public void setDeliveryLat(Double deliveryLat) { this.deliveryLat = deliveryLat; }
    public Double getDeliveryLng() { return deliveryLng; }
    public void setDeliveryLng(Double deliveryLng) { this.deliveryLng = deliveryLng; }
    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
    public String getDetailsJson() { return detailsJson; }
    public void setDetailsJson(String detailsJson) { this.detailsJson = detailsJson; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; this.updatedAt = Instant.now(); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
