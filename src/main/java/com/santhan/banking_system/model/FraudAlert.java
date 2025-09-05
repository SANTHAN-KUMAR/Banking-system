package com.santhan.banking_system.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "fraud_alerts")
public class FraudAlert {

    public enum AlertType {
        LARGE_TRANSACTION,
        MULTIPLE_LARGE_TRANSACTIONS,
        NEWLY_CREATED_ACCOUNT_TRANSFER,
        VELOCITY_CHECK,
        UNUSUAL_LOCATION,
        SUSPICIOUS_ACCOUNT_INTERACTION
    }

    public enum AlertStatus {
        PENDING("Pending Review"),
        REVIEWED("Reviewed"),
        DISMISSED("Dismissed"),
        ESCALATED("Escalated to Investigation");

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

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 50)
    private AlertType alertType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_updated_at") // NEW FIELD
    private Instant lastUpdatedAt; // NEW FIELD

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AlertStatus status;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public AlertType getAlertType() {
        return alertType;
    }

    public void setAlertType(AlertType alertType) {
        this.alertType = alertType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    // NEW Getter and Setter for lastUpdatedAt
    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(Instant lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }
}
