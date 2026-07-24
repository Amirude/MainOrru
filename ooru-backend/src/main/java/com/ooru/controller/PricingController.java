package com.ooru.controller;

import com.ooru.service.PricingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class PricingController {

    private final PricingService pricingService;

    public PricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    /** e.g. GET /api/pricing/surge?categoryCode=food — always returns the numbers behind the multiplier, not just the number. */
    @GetMapping("/api/pricing/surge")
    public ResponseEntity<Map<String, Object>> surge(@RequestParam String categoryCode) {
        var result = pricingService.surgeFor(categoryCode);
        return ResponseEntity.ok(Map.of(
                "multiplier", result.multiplier(),
                "activeBookings", result.activeBookings(),
                "approvedShops", result.approvedShops(),
                "explanation", result.explanation()
        ));
    }
}
