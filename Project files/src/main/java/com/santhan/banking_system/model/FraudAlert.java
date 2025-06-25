package com.santhan.banking_system.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "fraud_alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FraudAlert {

    public enum AlertStatus {
        PENDING("Pending Review"),
        REVIEWED("Reviewed"),
        DISMISSED("Dismissed"),
        ESCALATED("Escalated");

        private final String displayName;

        AlertStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(nullable = false)
    private String alertType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant lastUpdatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        this.lastUpdatedAt = this.createdAt;
        if (this.status == null) {
            this.status = AlertStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastUpdatedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
