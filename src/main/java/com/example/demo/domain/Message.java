package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- ADDED: unique = true to enforce webhook deduplication at the DB level ---
    @Column(unique = true)
    private String metaMid;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String messageType;

    @Enumerated(EnumType.STRING)
    private Direction direction;

    @Enumerated(EnumType.STRING)
    private SenderType senderType;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private Contact contact;

    public enum Direction {
        INBOUND, OUTBOUND
    }

    public enum SenderType {
        USER, BOT, AGENT
    }
}