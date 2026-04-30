package com.example.demo.repository;

import com.example.demo.domain.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintRepo extends JpaRepository<Complaint, Long> {

    // Used by AI to fetch only open complaints for the current shop
    List<Complaint> findByTenantIdAndStatus(Long tenantId, String status);
}