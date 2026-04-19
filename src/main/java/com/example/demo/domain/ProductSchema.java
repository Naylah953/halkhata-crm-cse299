package com.example.demo.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "product_schemas", uniqueConstraints = {
        // A shop cannot have two schemas with the exact same name (e.g., two "Clothing" schemas)
        @UniqueConstraint(columnNames = {"tenant_id", "name"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSchema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // e.g., "Clothing", "Electronics", "Food"

    // THE MAGIC FIELD: This maps to a PostgreSQL JSONB column natively!
    // It will store rules like: {"size": "string", "color": "string", "weight_kg": "number"}
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schema_definition", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> schemaDefinition;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}