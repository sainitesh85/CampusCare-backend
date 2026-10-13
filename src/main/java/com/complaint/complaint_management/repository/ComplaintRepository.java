
package com.complaint.complaint_management.repository;

import com.complaint.complaint_management.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository
        extends JpaRepository<Complaint, Long> {

    List<Complaint> findByStudentId(Long studentId);

    List<Complaint> findAllByOrderByCreatedAtDesc();
}
