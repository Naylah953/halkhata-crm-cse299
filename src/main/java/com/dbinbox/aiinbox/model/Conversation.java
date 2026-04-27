package com.dbinbox.aiinbox.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
public class Conversation
{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id; // Unique ID for this specific interaction session

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", referencedColumnName = "id", nullable = false)
    private Contact contact; // "id" here is the Meta PSID String

    @Enumerated(EnumType.STRING)
    private State state = State.AI;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String orderMetadata; // JSON string: {"sku": "...", "size": "..."}

    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL)
    private List<Message> messages;

    private Boolean isActive = true;

    public enum State
    {
        AI, ESCALATED, CONFIRM_ORDER, CLOSED
    }
}