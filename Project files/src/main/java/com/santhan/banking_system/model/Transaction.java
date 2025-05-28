package com.santhan.banking_system.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions") // This will be the name of the database table
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // What kind of transaction is it (Deposit, Withdrawal, Transfer)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    // The amount of money involved in the transaction
    @Column(nullable = false, precision = 19, scale = 4) // Precision for money
    private BigDecimal amount;

    // A description or note about the transaction
    @Column(length = 255)
    private String description;

    // When the transaction occurred
    @Column(nullable = false, updatable = false)
    private LocalDateTime transactionDate;

    // Who initiated the transaction (could be the user themselves)
    // Optional: We can link this to a User entity if needed, but for now, let's keep it simple.
    // private Long initiatedByUserId;

    // --- Relationships to Accounts ---

    // The account from which money is withdrawn or transferred (Source Account)
    // For a deposit, this could be null or represent the 'from' external source.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id") // Foreign key column in 'transactions' table
    private Account sourceAccount;

    // The account to which money is deposited or transferred (Destination Account)
    // For a withdrawal, this could be null or represent the 'to' external destination.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_account_id") // Foreign key column in 'transactions' table
    private Account destinationAccount;

    // Constructors for convenience (Lombok handles NoArgsConstructor and AllArgsConstructor)
    public Transaction(TransactionType transactionType, BigDecimal amount, String description, Account sourceAccount, Account destinationAccount) {
        this.transactionType = transactionType;
        this.amount = amount;
        this.description = description;
        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
        this.transactionDate = LocalDateTime.now(); // Set transaction date on creation
    }

    // You might want to add a constructor just for deposits/withdrawals where one account is null
    public Transaction(TransactionType transactionType, BigDecimal amount, String description, Account account) {
        this.transactionType = transactionType;
        this.amount = amount;
        this.description = description;
        if (transactionType == TransactionType.DEPOSIT) {
            this.destinationAccount = account;
            this.sourceAccount = null; // No source account for a simple deposit
        } else if (transactionType == TransactionType.WITHDRAWAL) {
            this.sourceAccount = account;
            this.destinationAccount = null; // No destination account for a simple withdrawal
        } else {
            throw new IllegalArgumentException("This constructor is only for DEPOSIT or WITHDRAWAL types.");
        }
        this.transactionDate = LocalDateTime.now();
    }


    // Lifecycle callback for transactionDate (though constructor handles it, good for clarity)
    @PrePersist
    protected void onCreate() {
        if (this.transactionDate == null) {
            this.transactionDate = LocalDateTime.now();
        }
    }
}