package com.example.demo.repository;

import com.example.demo.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepo extends JpaRepository<Message, Long> {

    // NEW MULTI-TENANT RULE: Only fetch messages where the linked Contact belongs to the given Tenant ID
    List<Message> findByContact_Tenant_IdOrderByCreatedAtDesc(Long tenantId);
}