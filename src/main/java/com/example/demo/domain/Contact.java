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
    // REAL-TIME UI FIELDS
    // ==========================================

    @Column(name = "unread_count")
    private Integer unreadCount = 0;

    @Column(name = "requires_human")
    private Boolean requiresHuman = false;

    @Column(name = "order_ready")
    private Boolean orderReady = false;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    // ==========================================
    // NEW FIELDS FOR AI MODERATOR TOOLS
    // ==========================================

    // Store comma-separated tags (e.g., "VIP, Spammer, Wholesaler")
    @Column(name = "tags")
    private String tags;

    // TEXT type to hold long AI-generated profile summaries or human notes
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public void setAdminBriefing(String aiSummary) {
        this.aiSummary = aiSummary;
    }
}