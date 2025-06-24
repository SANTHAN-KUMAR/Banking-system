package com.santhan.banking_system.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
// Removed @AllArgsConstructor for clarity as we use custom constructor and handle hash generation in service
import java.math.BigDecimal;
import java.time.Instant; // NEW: Import Instant
import java.time.temporal.ChronoUnit; // Import ChronoUnit for truncation

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String description;

    // CHANGED: From LocalDateTime to Instant
    // @Column(nullable = false, updatable = false) // Removed updatable=false as Instant can be set initially
    @Column(nullable = false) // Ensures column is not null
    private Instant transactionDate; // Use Instant for timezone-independent timestamp

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id")
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_account_id")
    private Account destinationAccount;

    @Column(length = 64)
    private String transactionHash;

    @Column(length = 64)
    private String previousTransactionHash;

    // Custom constructor
    public Transaction(TransactionType transactionType, BigDecimal amount, String description,
                       Account sourceAccount, Account destinationAccount) {
        this.transactionType = transactionType;
        this.amount = amount;
        this.description = description;
        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
        // Set Instant.now() initially. @PrePersist will handle truncation for DB consistency.
        this.transactionDate = Instant.now();
    }

    // NEW/MODIFIED: Lifecycle callback to truncate Instant to seconds before persisting
    @PrePersist
    protected void onCreate() {
        if (this.transactionDate == null) {
            this.transactionDate = Instant.now();
        }
        // CRITICAL FIX: Truncate to seconds (or milliseconds if your DB supports it consistently)
        // Instant is inherently UTC. Truncating ensures precision consistency with DB storage.
        this.transactionDate = this.transactionDate.truncatedTo(ChronoUnit.SECONDS);
    }
}
