package com.example.demo.repository;

import com.example.demo.domain.Contact;
import com.example.demo.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepo extends JpaRepository<Message, Long> {

    // Existing methods...
    List<Message> findByContact_Tenant_IdOrderByCreatedAtDesc(Long tenantId);
    List<Message> findTop10ByContactOrderByCreatedAtDesc(Contact contact);
    boolean existsByMetaMid(String metaMid);

    // ==========================================
    // NEW FOR AI TOOL: getDetailedChatLogs
    // ==========================================
    List<Message> findTop10ByContact_IdAndContact_Tenant_IdOrderByCreatedAtDesc(String contactId, Long tenantId);
}