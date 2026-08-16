package com.expensetracker.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a monthly spending budget set by a user.
 */
@Data
@NoArgsConstructor
@Entity
@Table(
    name = "budget",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_budget_user_month", columnNames = {"user_id", "budget_month"})
    },
    indexes = {
        @Index(name = "idx_budget_user_month", columnList = "user_id, budget_month")
    }
)
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;

    @Column(name = "budget_month", nullable = false, length = 7)
    private String yearMonth; // Format: "YYYY-MM"

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
