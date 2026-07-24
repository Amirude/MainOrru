package com.ooru.service;

import com.ooru.dto.ComplaintDtos.*;
import com.ooru.model.Booking;
import com.ooru.model.Complaint;
import com.ooru.model.User;
import com.ooru.repository.BookingRepository;
import com.ooru.repository.ComplaintRepository;
import com.ooru.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    public ComplaintService(ComplaintRepository complaintRepository, UserRepository userRepository,
                             BookingRepository bookingRepository) {
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
    }

    public Complaint create(Long userId, CreateComplaintRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        Complaint complaint = new Complaint();
        complaint.setRaisedBy(user);
        complaint.setSubject(req.subject);
        complaint.setDescription(req.description);
        if (req.bookingId != null) {
            Booking booking = bookingRepository.findById(req.bookingId)
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
            complaint.setBooking(booking);
        }
        return complaintRepository.save(complaint);
    }

    public List<Complaint> mine(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        return complaintRepository.findByRaisedByOrderByCreatedAtDesc(user);
    }

    public List<Complaint> all() {
        return complaintRepository.findAllByOrderByCreatedAtDesc();
    }

    public Complaint respond(Long complaintId, RespondRequest req) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found"));
        complaint.setStatus(Complaint.Status.valueOf(req.status));
        if (req.adminResponse != null) complaint.setAdminResponse(req.adminResponse);
        if (complaint.getStatus() == Complaint.Status.RESOLVED) complaint.setResolvedAt(Instant.now());
        return complaintRepository.save(complaint);
    }

    public ComplaintResponse toResponse(Complaint c) {
        ComplaintResponse res = new ComplaintResponse();
        res.id = c.getId();
        res.raisedByName = c.getRaisedBy().getName();
        res.bookingId = c.getBooking() != null ? c.getBooking().getId() : null;
        res.bookingReference = c.getBooking() != null ? c.getBooking().getReference() : null;
        res.subject = c.getSubject();
        res.description = c.getDescription();
        res.status = c.getStatus().name();
        res.adminResponse = c.getAdminResponse();
        res.createdAt = c.getCreatedAt();
        res.resolvedAt = c.getResolvedAt();
        return res;
    }
}
