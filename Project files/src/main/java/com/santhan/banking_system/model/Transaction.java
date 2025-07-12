package com.santhan.banking_system.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

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

    @Column(nullable = false)
    private Instant transactionDate;

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

    @Column(nullable = false)
    private String status = "COMPLETED"; // Default status for new transactions

    // NEW: Fields for transaction reversal
    @Column(nullable = false)
    private boolean reversed = false; // Indicates if this specific transaction has been reversed

    @Column(name = "original_transaction_id")
    private Long originalTransactionId; // Links reversal transaction to its original, or null if it's an original

    // Custom constructor
    public Transaction(TransactionType transactionType, BigDecimal amount, String description,
                       Account sourceAccount, Account destinationAccount) {
        this.transactionType = transactionType;
        this.amount = amount;
        this.description = description;
        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
        this.transactionDate = Instant.now();
        this.status = "COMPLETED"; // New transactions are completed by default
        this.reversed = false; // Not reversed by default
        this.originalTransactionId = null; // No original transaction by default
    }

    @PrePersist
    protected void onCreate() {
        if (this.transactionDate == null) {
            this.transactionDate = Instant.now();
        }
        this.transactionDate = this.transactionDate.truncatedTo(ChronoUnit.SECONDS);
        if (this.status == null || this.status.isEmpty()) {
            this.status = "COMPLETED"; // Ensure status is set on persist if not already
        }
        // `reversed` and `originalTransactionId` are handled by constructor or setters
    }
}
