package com.dbinbox.aiinbox.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Data
@Table(name = "customer_orders")
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String orderSummary;
    private String status = "DRAFT";

    @ManyToOne
    @JoinColumn(name = "contact_id")
    private Contact contact; // Linked to your existing Contact entity

    @ManyToOne
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    // Getters and Setters...
}