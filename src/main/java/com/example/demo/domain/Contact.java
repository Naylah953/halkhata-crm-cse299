package com.example.demo.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contacts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Contact {

    @Id
    private String id; // Facebook PSID
    private String name;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    // ==========================================
    // NEW FIELD: Link to the Customer profile
    // ==========================================
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id") // Nullable by default
    private Customer customer;

    // --- THE FIX: Forces Jackson to output "customerId": X in the JSON ---
    @JsonProperty("customerId")
    public Long getCustomerId() {
        return this.customer != null ? this.customer.getId() : null;
    }

    @JsonIgnore
    @OneToMany(mappedBy = "contact", cascade = CascadeType.ALL)
    private List<Message> messageList = new ArrayList<>();

    // ==========================================
    // NEW REAL-TIME UI FIELDS
    // ==========================================

    // Changed 'int' to 'Integer' to prevent null mapping errors
    @Column(name = "unread_count")
    private Integer unreadCount = 0;

    // Changed 'boolean' to 'Boolean' to prevent null mapping errors
    @Column(name = "requires_human")
    private Boolean requiresHuman = false;

    // Changed 'boolean' to 'Boolean' to prevent null mapping errors
    @Column(name = "order_ready")
    private Boolean orderReady = false;
}