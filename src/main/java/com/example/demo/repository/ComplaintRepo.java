package com.example.demo.repository;

import com.example.demo.domain.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintRepo extends JpaRepository<Complaint, Long> {

    // Used by AI to fetch only open complaints for the current shop
    List<Complaint> findByTenantIdAndStatus(Long tenantId, String status);

    // Fetch all complaints for a specific customer in a specific shop
    List<Complaint> findByPsidAndTenantIdOrderByCreatedAtDesc(String psid, Long tenantId);

    // Phase 4: NLP Complaint Resolution (Fuzzy Search)
    // Translates to: SELECT * FROM complaints WHERE tenant_id = ? AND description ILIKE '%?%'
    List<Complaint> findByTenantIdAndDescriptionContainingIgnoreCase(Long tenantId, String keyword);
}