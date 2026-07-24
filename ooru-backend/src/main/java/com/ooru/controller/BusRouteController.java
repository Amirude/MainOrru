package com.ooru.controller;

import com.ooru.model.BusRoute;
import com.ooru.repository.BusRouteRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bus-routes")
public class BusRouteController {

    private final BusRouteRepository busRouteRepository;

    public BusRouteController(BusRouteRepository busRouteRepository) {
        this.busRouteRepository = busRouteRepository;
    }

    /** e.g. GET /api/bus-routes/search?q=tambaram or ?q=21G */
    @GetMapping("/search")
    public ResponseEntity<List<BusRoute>> search(@RequestParam String q) {
        return ResponseEntity.ok(busRouteRepository.search(q));
    }
}
