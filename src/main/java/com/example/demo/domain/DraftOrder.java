package com.example.demo.domain;

import com.example.demo.domain.enums.DeliveryMethod;
import com.example.demo.domain.enums.PaymentMethod;
import com.fasterxml.jackson.annotation.JsonIgnore; // <-- ADDED
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "draft_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DraftOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Structured Customer Payload
    private String providedName;
    private String providedPhone;
    private String providedEmail;
    private String providedAddress;

    // Structured Order Payload
    @Enumerated(EnumType.STRING)
    private DeliveryMethod deliveryMethod;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    public enum DraftStatus { PENDING, CONFIRMED, CANCELLED }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DraftStatus status = DraftStatus.PENDING;

    // <-- ADDED @JsonIgnore to prevent infinite recursion
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;

    // <-- ADDED @JsonIgnore to prevent infinite recursion
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @OneToMany(mappedBy = "draftOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DraftOrderItem> items = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    public void addDraftItem(DraftOrderItem item) {
        items.add(item);
        item.setDraftOrder(this);
    }
}