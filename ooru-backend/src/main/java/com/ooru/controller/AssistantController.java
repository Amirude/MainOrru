package com.ooru.controller;

import com.ooru.dto.AssistantDtos.*;
import com.ooru.dto.BookingDtos.BookingResponse;
import com.ooru.dto.BookingDtos.CreateBookingRequest;
import com.ooru.model.Booking;
import com.ooru.service.AssistantService;
import com.ooru.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantService assistantService;
    private final BookingService bookingService;

    public AssistantController(AssistantService assistantService, BookingService bookingService) {
        this.assistantService = assistantService;
        this.bookingService = bookingService;
    }

    @GetMapping("/schema")
    public ResponseEntity<?> schema() {
        return ResponseEntity.ok(AssistantService.SCHEMA);
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest req) throws Exception {
        return ResponseEntity.ok(assistantService.chat(req));
    }

    /** Turns a "ready" assistant state into a real booking — the exact same path Home.jsx's manual form uses. */
    @PostMapping("/confirm")
    public ResponseEntity<BookingResponse> confirm(Authentication auth, @Valid @RequestBody ConfirmRequest req) {
        Long customerUserId = (Long) auth.getPrincipal();
        CreateBookingRequest bookingReq = new CreateBookingRequest();
        bookingReq.categoryCode = req.category;
        bookingReq.details = req.data;
        Booking booking = bookingService.create(customerUserId, bookingReq);
        return ResponseEntity.ok(bookingService.toResponse(booking));
    }
}
