package com.ooru.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * A review can only exist against a booking that's actually COMPLETED (see ReviewService) — this
 * is what "verified review" means here: you can't review a shop you never actually used.
 */
@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @ManyToOne(optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Column(nullable = false)
    private int rating; // 1-5, for the shop

    @ManyToOne
    @JoinColumn(name = "delivery_partner_id")
    private User deliveryPartner; // set only if this booking had one

    private Integer deliveryPartnerRating; // 1-5, optional — null if there was no delivery to rate

    @Column(length = 1000)
    private String comment;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Review() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public Shop getShop() { return shop; }
    public void setShop(Shop shop) { this.shop = shop; }
    public User getCustomer() { return customer; }
    public void setCustomer(User customer) { this.customer = customer; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public User getDeliveryPartner() { return deliveryPartner; }
    public void setDeliveryPartner(User deliveryPartner) { this.deliveryPartner = deliveryPartner; }
    public Integer getDeliveryPartnerRating() { return deliveryPartnerRating; }
    public void setDeliveryPartnerRating(Integer deliveryPartnerRating) { this.deliveryPartnerRating = deliveryPartnerRating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Instant getCreatedAt() { return createdAt; }
}
