package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The Facebook User who complained
    @Column(nullable = false)
    private String psid;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    // e.g., "HIGH", "MEDIUM", "LOW"
    @Column(nullable = false)
    private String priority;

    // e.g., "OPEN", "RESOLVED"
    @Column(nullable = false)
    private String status;

    // Strict multi-tenant isolation
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}