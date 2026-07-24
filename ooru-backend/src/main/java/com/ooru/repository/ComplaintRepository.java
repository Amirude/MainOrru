package com.ooru.repository;

import com.ooru.model.Complaint;
import com.ooru.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByRaisedByOrderByCreatedAtDesc(User raisedBy);
    List<Complaint> findAllByOrderByCreatedAtDesc();
}
