package com.dbinbox.aiinbox.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp; // Useful for auto-dating

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Industry standard uses Long for IDs

    private String metaMid; // Renamed to camelCase for Java standards

    @Column(columnDefinition = "TEXT") // Allows for very long messages/URLs
    private String content;

    private String messageType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    @JsonIgnore
    private Conversation conversation;

    @Enumerated(EnumType.STRING) // Saves as "INBOUND" in DB
    private Direction direction;

    @Enumerated(EnumType.STRING) // Saves as "BOT" in DB
    private SenderType senderType;

    @CreationTimestamp // Automatically sets the time when saved
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "contact_id")
    private Contact contact;


    // Defined as public enums (can be in this file or separate ones)
    public enum Direction
    {
        INBOUND, OUTBOUND
    }

    public enum SenderType
    {
        USER, ADMIN, AI
    }

    @JsonProperty("is_echo") Boolean isEcho; // CRITICAL: To ignore messages sent BY your page

}