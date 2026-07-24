package com.ooru.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ooru.dto.BookingDtos.*;
import com.ooru.dto.MenuDtos.OrderItemRequest;
import com.ooru.model.*;
import com.ooru.repository.BookingRepository;
import com.ooru.repository.MenuItemRepository;
import com.ooru.repository.ServiceCategoryRepository;
import com.ooru.repository.ShopRepository;
import com.ooru.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final ServiceCategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final SlotService slotService;
    private final NotificationService notificationService;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    private final com.ooru.repository.ReviewRepository reviewRepository;
    private final com.ooru.repository.PaymentRepository paymentRepository;
    private final PricingService pricingService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ID_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final java.util.Set<String> CART_BASED_CATEGORIES = java.util.Set.of("food", "grocery", "fashion");
    private static final long BASE_DELIVERY_FEE_PAISE = 2500; // ₹25 base, before surge

    public BookingService(BookingRepository bookingRepository, UserRepository userRepository,
                           ShopRepository shopRepository, ServiceCategoryRepository categoryRepository,
                           MenuItemRepository menuItemRepository, SlotService slotService,
                           NotificationService notificationService,
                           org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate,
                           com.ooru.repository.ReviewRepository reviewRepository,
                           com.ooru.repository.PaymentRepository paymentRepository,
                           PricingService pricingService) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.shopRepository = shopRepository;
        this.categoryRepository = categoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.slotService = slotService;
        this.notificationService = notificationService;
        this.messagingTemplate = messagingTemplate;
        this.reviewRepository = reviewRepository;
        this.paymentRepository = paymentRepository;
        this.pricingService = pricingService;
    }

    public Booking create(Long customerUserId, CreateBookingRequest req) {
        ServiceCategory category = categoryRepository.findByCode(req.categoryCode)
                .filter(ServiceCategory::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Unknown or inactive service category: " + req.categoryCode));

        User customer = userRepository.findById(customerUserId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        Booking booking = new Booking();
        booking.setReference(generateReference());
        booking.setCustomer(customer);
        booking.setCategoryCode(category.getCode());
        booking.setStatus(BookingStatus.REQUESTED);

        Shop shop = null;
        if (req.shopId != null) {
            shop = shopRepository.findById(req.shopId)
                    .filter(s -> s.getStatus() == Shop.ShopStatus.APPROVED)
                    .orElseThrow(() -> new IllegalArgumentException("Shop not found or not approved"));
            booking.setShop(shop);
        }

        if (req.slotId != null) {
            if (shop == null) {
                throw new IllegalArgumentException("A shop must be selected to book a slot");
            }
            AppointmentSlot slot = slotService.claim(req.slotId, shop.getId());
            booking.setSlot(slot);
        }

        if (CART_BASED_CATEGORIES.contains(category.getCode())) {
            if (shop == null) {
                throw new IllegalArgumentException("A shop must be selected for food/grocery orders");
            }
            if (req.items == null || req.items.isEmpty()) {
                throw new IllegalArgumentException("Cart is empty — add at least one item");
            }
            booking.setDetailsJson(toJson(buildCartDetails(shop, req)));
        } else {
            // Simple field-based categories (tailor, xerox, ac, plumber, electrician, parcel, rental, driver, ...)
            // just pass their form fields straight through as strings, plus the claimed slot if any.
            Map<String, Object> details = new LinkedHashMap<>(req.details != null ? req.details : Map.of());
            if (booking.getSlot() != null) {
                AppointmentSlot slot = booking.getSlot();
                details.put("appointmentDate", slot.getDate().toString());
                details.put("appointmentTime", slot.getStartTime() + " - " + slot.getEndTime());
            }
            booking.setDetailsJson(toJson(details));
        }

        Booking saved = bookingRepository.save(booking);

        if (saved.getShop() != null) {
            notificationService.notifyShopOfNewBooking(saved);
        }
        if (DELIVERABLE_CATEGORIES.contains(saved.getCategoryCode()) && saved.getDeliveryPartner() == null) {
            // Lets DeliveryDashboard.jsx react live instead of polling every few seconds.
            messagingTemplate.convertAndSend("/topic/deliveries/available", toResponse(saved));
        }

        return saved;
    }

    /** Looks up each cart item's CURRENT price from the shop's menu — never trusts a price the client might send. */
    private Map<String, Object> buildCartDetails(Shop shop, CreateBookingRequest req) {
        List<Map<String, Object>> lineItems = new ArrayList<>();
        long totalPaise = 0;

        for (OrderItemRequest item : req.items) {
            MenuItem menuItem = menuItemRepository.findById(item.menuItemId)
                    .filter(mi -> mi.getShop().getId().equals(shop.getId()) && mi.isActive())
                    .orElseThrow(() -> new IllegalArgumentException("Menu item " + item.menuItemId + " is not available at this shop"));

            long lineTotal = menuItem.getPricePaise() * item.quantity;
            totalPaise += lineTotal;

            Map<String, Object> line = new LinkedHashMap<>();
            line.put("menuItemId", menuItem.getId());
            line.put("name", menuItem.getName());
            line.put("quantity", item.quantity);
            line.put("unitPricePaise", menuItem.getPricePaise());
            line.put("lineTotalPaise", lineTotal);
            line.put("imageUrl", menuItem.getImageUrl());
            lineItems.add(line);
        }

        Map<String, Object> details = new LinkedHashMap<>(req.details != null ? req.details : Map.of());
        details.put("items", lineItems);
        details.put("itemsSubtotalPaise", totalPaise);

        // Real distance-based delivery fee: ₹10/km between the shop and the customer's actual
        // checkout location, with a floor so a 200m order isn't charged almost nothing. Falls
        // back to a flat estimate if either coordinate is missing (e.g. location permission
        // denied) — see PER_KM_FEE_PAISE / MIN_DELIVERY_FEE_PAISE below.
        long baseDeliveryFeePaise;
        Double distanceKm = null;
        if (shop.getLatitude() != null && shop.getLongitude() != null
                && req.customerLat != null && req.customerLng != null) {
            distanceKm = haversineKm(shop.getLatitude(), shop.getLongitude(), req.customerLat, req.customerLng);
            baseDeliveryFeePaise = Math.max(MIN_DELIVERY_FEE_PAISE, Math.round(distanceKm * PER_KM_FEE_PAISE));
        } else {
            baseDeliveryFeePaise = BASE_DELIVERY_FEE_PAISE;
        }

        // Real dynamic pricing: the delivery fee moves with actual current demand vs. supply for
        // this category — see PricingService for the exact, published formula. Not hidden, not ML.
        var surge = pricingService.surgeFor(req.categoryCode);
        long deliveryFeePaise = pricingService.applySurge(baseDeliveryFeePaise, surge.multiplier());
        details.put("deliveryFeePaise", deliveryFeePaise);
        details.put("deliveryDistanceKm", distanceKm);
        details.put("surgeMultiplier", surge.multiplier());
        details.put("surgeExplanation", surge.explanation());

        // GST and handling charge are set by the shop owner (see Shop.gstPercent / handlingFeePaise,
        // and PATCH /api/shops/{id}/billing-settings) — null/0 means the shop doesn't charge them.
        long gstPaise = shop.getGstPercent() != null ? Math.round(totalPaise * shop.getGstPercent() / 100.0) : 0;
        long handlingFeePaise = shop.getHandlingFeePaise() != null ? shop.getHandlingFeePaise() : 0;
        details.put("gstPaise", gstPaise);
        details.put("gstPercent", shop.getGstPercent());
        details.put("handlingFeePaise", handlingFeePaise);

        details.put("paymentMethod", req.paymentMethod != null ? req.paymentMethod : "ONLINE");
        details.put("totalPaise", totalPaise + deliveryFeePaise + gstPaise + handlingFeePaise);
        return details;
    }

    private static final long PER_KM_FEE_PAISE = 1000; // ₹10/km, the real published rate
    private static final long MIN_DELIVERY_FEE_PAISE = 1500; // floor so very short trips aren't nearly free

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public List<Booking> myBookings(Long customerUserId) {
        User customer = userRepository.findById(customerUserId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        return bookingRepository.findByCustomerOrderByCreatedAtDesc(customer);
    }

    /**
     * "Order again" — built entirely from this ONE customer's own past food/grocery orders, no
     * cross-customer data and no ML model. Just: which menu items have they ordered before, and
     * how often. Honest personalization, not a black box.
     */
    public List<Map<String, Object>> frequentlyOrderedItems(Long customerUserId, int limit) {
        User customer = userRepository.findById(customerUserId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        Map<Long, Integer> countByMenuItemId = new LinkedHashMap<>();
        for (Booking b : bookingRepository.findByCustomerOrderByCreatedAtDesc(customer)) {
            if (!CART_BASED_CATEGORIES.contains(b.getCategoryCode())) continue;
            Map<String, Object> details = parseDetails(b);
            Object itemsObj = details.get("items");
            if (!(itemsObj instanceof List<?> items)) continue;
            for (Object itemObj : items) {
                if (!(itemObj instanceof Map<?, ?> item)) continue;
                Object idObj = item.get("menuItemId");
                if (idObj == null) continue;
                Long menuItemId = ((Number) idObj).longValue();
                int qty = item.get("quantity") instanceof Number n ? n.intValue() : 1;
                countByMenuItemId.merge(menuItemId, qty, Integer::sum);
            }
        }

        return countByMenuItemId.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(e -> menuItemRepository.findById(e.getKey()).filter(MenuItem::isActive).map(mi -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("menuItemId", mi.getId());
                    row.put("name", mi.getName());
                    row.put("pricePaise", mi.getPricePaise());
                    row.put("imageUrl", mi.getImageUrl());
                    row.put("shopId", mi.getShop().getId());
                    row.put("shopName", mi.getShop().getShopName());
                    row.put("categoryCode", mi.getShop().getCategoryCode());
                    row.put("timesOrdered", e.getValue());
                    return row;
                }).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public List<Booking> shopBookings(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalStateException("Shop not found"));
        return bookingRepository.findByShopOrderByCreatedAtDesc(shop);
    }

    private static final java.util.Set<String> DELIVERABLE_CATEGORIES = java.util.Set.of("food", "grocery", "parcel");

    /** Bookings a delivery partner could pick up — only ones nobody has claimed yet. */
    public List<Booking> availableDeliveries() {
        return bookingRepository.findByDeliveryPartnerIsNullAndCategoryCodeInOrderByCreatedAtDesc(
                new java.util.ArrayList<>(DELIVERABLE_CATEGORIES));
    }

    public List<Booking> myDeliveries(Long deliveryPartnerUserId) {
        User partner = userRepository.findById(deliveryPartnerUserId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        return bookingRepository.findByDeliveryPartnerOrderByCreatedAtDesc(partner);
    }

    public Booking claimDelivery(Long deliveryPartnerUserId, Long bookingId) {
        User partner = userRepository.findById(deliveryPartnerUserId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        if (partner.getRole() != Role.DELIVERY_PARTNER) {
            throw new IllegalStateException("Only delivery partner accounts can claim deliveries");
        }
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalStateException("Booking not found"));
        if (booking.getDeliveryPartner() != null) {
            throw new IllegalStateException("This delivery was just claimed by someone else");
        }
        booking.setDeliveryPartner(partner);
        Booking saved = bookingRepository.save(booking);
        messagingTemplate.convertAndSend(
                "/topic/customer/" + saved.getCustomer().getId() + "/bookings", toResponse(saved));
        return saved;
    }

    /** Called repeatedly (every few seconds) by the delivery partner's own device while en route. */
    public Booking updateDeliveryLocation(Long deliveryPartnerUserId, Long bookingId, double lat, double lng) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalStateException("Booking not found"));
        if (booking.getDeliveryPartner() == null || !booking.getDeliveryPartner().getId().equals(deliveryPartnerUserId)) {
            throw new IllegalStateException("You haven't claimed this delivery");
        }
        booking.setDeliveryLat(lat);
        booking.setDeliveryLng(lng);
        Booking saved = bookingRepository.save(booking);
        // Broadcast on every ping — this is what makes the customer's map marker move live.
        messagingTemplate.convertAndSend(
                "/topic/customer/" + saved.getCustomer().getId() + "/bookings", toResponse(saved));
        return saved;
    }

    public Booking updateStatus(Long bookingId, BookingStatus newStatus) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalStateException("Booking not found"));
        booking.setStatus(newStatus);
        Booking saved = bookingRepository.save(booking);
        notificationService.notifyBookingStatusChanged(saved);
        messagingTemplate.convertAndSend(
                "/topic/customer/" + saved.getCustomer().getId() + "/bookings",
                toResponse(saved));
        return saved;
    }

    public Map<String, Object> receiptFor(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalStateException("Booking not found"));
        Map<String, Object> receipt = new LinkedHashMap<>();
        receipt.put("booking", toResponse(booking));
        paymentRepository.findByBooking(booking).ifPresentOrElse(
                payment -> receipt.put("payment", Map.of(
                        "amountPaise", payment.getAmountPaise(),
                        "currency", payment.getCurrency(),
                        "status", payment.getStatus().name(),
                        "razorpayPaymentId", payment.getRazorpayPaymentId() != null ? payment.getRazorpayPaymentId() : "",
                        "createdAt", payment.getCreatedAt().toString()
                )),
                () -> receipt.put("payment", null)
        );
        return receipt;
    }

    /** Real stats from this delivery partner's own completed deliveries — no estimates. */
    public Map<String, Object> deliveryStats(Long deliveryPartnerUserId) {
        User partner = userRepository.findById(deliveryPartnerUserId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        List<Booking> completed = bookingRepository.findByDeliveryPartnerOrderByCreatedAtDesc(partner).stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .toList();

        Instant now = Instant.now();
        Instant startOfDay = now.truncatedTo(java.time.temporal.ChronoUnit.DAYS);
        Instant startOfWeek = startOfDay.minus(java.time.Duration.ofDays(now.atZone(java.time.ZoneOffset.UTC).getDayOfWeek().getValue() - 1L));
        Instant startOfMonth = now.atZone(java.time.ZoneOffset.UTC).withDayOfMonth(1).toLocalDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        Instant startOfYear = now.atZone(java.time.ZoneOffset.UTC).withDayOfYear(1).toLocalDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("today", windowStats(completed, startOfDay));
        stats.put("thisWeek", windowStats(completed, startOfWeek));
        stats.put("thisMonth", windowStats(completed, startOfMonth));
        stats.put("thisYear", windowStats(completed, startOfYear));
        stats.put("allTime", windowStats(completed, Instant.EPOCH));
        return stats;
    }

    private Map<String, Object> windowStats(List<Booking> completed, Instant since) {
        long orders = 0;
        long earningsPaise = 0;
        for (Booking b : completed) {
            if (b.getUpdatedAt().isBefore(since)) continue;
            orders++;
            Object fee = parseDetails(b).get("deliveryFeePaise");
            if (fee instanceof Number n) earningsPaise += n.longValue();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orders", orders);
        result.put("earningsPaise", earningsPaise);
        return result;
    }

    public Booking assignShop(Long bookingId, Long shopId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalStateException("Booking not found"));
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalStateException("Shop not found"));
        booking.setShop(shop);
        return bookingRepository.save(booking);
    }

    /**
     * Lets the customer choose pickup vs. delivery — but only after the shop has actually marked
     * the item COMPLETED (ready). Choosing this earlier wouldn't mean anything yet.
     */
    public Booking setFulfillment(Long customerUserId, Long bookingId, FulfillmentRequest req) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalStateException("Booking not found"));
        if (!booking.getCustomer().getId().equals(customerUserId)) {
            throw new IllegalStateException("This isn't your booking");
        }
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalStateException("This booking isn't ready yet");
        }
        if (!"PICKUP".equals(req.method) && !"DELIVERY".equals(req.method)) {
            throw new IllegalArgumentException("method must be PICKUP or DELIVERY");
        }
        if ("DELIVERY".equals(req.method) && (req.address == null || req.address.isBlank())) {
            throw new IllegalArgumentException("An address is required for delivery");
        }

        Map<String, Object> details = new LinkedHashMap<>(parseDetails(booking));
        details.put("fulfillmentMethod", req.method);
        if (req.address != null) details.put("fulfillmentAddress", req.address);
        booking.setDetailsJson(toJson(details));
        return bookingRepository.save(booking);
    }

    public Map<String, Object> parseDetails(Booking booking) {
        try {
            return objectMapper.readValue(booking.getDetailsJson(), new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Corrupted booking details", e);
        }
    }

    /** Shared mapper so BookingController and AssistantController produce identical response shapes. */
    public BookingResponse toResponse(Booking booking) {
        BookingResponse res = new BookingResponse();
        res.id = booking.getId();
        res.reference = booking.getReference();
        res.categoryCode = booking.getCategoryCode();
        res.details = parseDetails(booking);
        res.status = booking.getStatus();
        res.shopId = booking.getShop() != null ? booking.getShop().getId() : null;
        res.shopName = booking.getShop() != null ? booking.getShop().getShopName() : null;
        res.hasReview = reviewRepository.existsByBooking(booking);
        res.customerPhone = booking.getCustomer().getPhone();
        res.deliveryPartnerName = booking.getDeliveryPartner() != null ? booking.getDeliveryPartner().getName() : null;
        res.deliveryLat = booking.getDeliveryLat();
        res.deliveryLng = booking.getDeliveryLng();
        res.createdAt = booking.getCreatedAt();
        res.updatedAt = booking.getUpdatedAt();
        return res;
    }

    private String toJson(Map<String, ?> details) {
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not serialize booking details", e);
        }
    }

    private String generateReference() {
        String suffix = RANDOM.ints(5, 0, ID_CHARS.length())
                .mapToObj(ID_CHARS::charAt)
                .map(String::valueOf)
                .collect(Collectors.joining());
        return "OOR-" + suffix;
    }
}
