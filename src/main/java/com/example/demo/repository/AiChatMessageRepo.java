package com.example.demo.repository;

import com.example.demo.domain.AiChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface AiChatMessageRepo extends JpaRepository<AiChatMessage, Long> {

    @Transactional
    void deleteByTenantId(Long tenantId);

    // Custom query to fetch a specific shop's AI chat history in chronological order
    List<AiChatMessage> findByTenantIdOrderByCreatedAtAsc(Long tenantId);
}

