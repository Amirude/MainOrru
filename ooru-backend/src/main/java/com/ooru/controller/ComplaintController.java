package com.ooru.controller;

import com.ooru.dto.ComplaintDtos.*;
import com.ooru.model.Complaint;
import com.ooru.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @PostMapping("/api/complaints")
    public ResponseEntity<ComplaintResponse> create(Authentication auth, @Valid @RequestBody CreateComplaintRequest req) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(complaintService.toResponse(complaintService.create(userId, req)));
    }

    @GetMapping("/api/complaints/mine")
    public ResponseEntity<List<ComplaintResponse>> mine(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(complaintService.mine(userId).stream().map(complaintService::toResponse).toList());
    }

    @GetMapping("/api/admin/complaints")
    public ResponseEntity<List<ComplaintResponse>> all() {
        return ResponseEntity.ok(complaintService.all().stream().map(complaintService::toResponse).toList());
    }

    @PatchMapping("/api/admin/complaints/{id}")
    public ResponseEntity<ComplaintResponse> respond(@PathVariable Long id, @Valid @RequestBody RespondRequest req) {
        Complaint complaint = complaintService.respond(id, req);
        return ResponseEntity.ok(complaintService.toResponse(complaint));
    }
}
