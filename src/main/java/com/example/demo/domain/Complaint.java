package com.example.demo.domain;

import jakarta.persistence.*;
        import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
@Data
public class Complaint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String psid;
    private String description;
    private String priority; // HIGH, MEDIUM, LOW
    private String status;   // OPEN, RESOLVED
    private Long tenantId;
    private LocalDateTime createdAt = LocalDateTime.now();
}