package com.example.demo.repository;

import com.example.demo.domain.DraftOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DraftOrderRepo extends JpaRepository<DraftOrder, Long> {
    List<DraftOrder> findByTenantId(Long tenantId);
    Optional<DraftOrder> findByIdAndTenantId(Long id, Long tenantId);

    // Crucial for the modal UI
    List<DraftOrder> findByContactIdAndTenantIdAndStatus(String contactId, Long tenantId, DraftOrder.DraftStatus status);
}