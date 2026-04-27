package com.example.demo.repository;

import com.example.demo.domain.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ComplaintRepo extends JpaRepository<Complaint, Long> {
    List<Complaint> findByTenantIdAndStatus(Long tenantId, String status);
}