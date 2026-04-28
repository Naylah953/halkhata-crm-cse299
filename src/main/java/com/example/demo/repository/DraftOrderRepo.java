package com.example.demo.repository;

import com.example.demo.domain.DraftOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DraftOrderRepo extends JpaRepository<DraftOrder, Long> {
    List<DraftOrder> findByTenantId(Long tenantId);
}