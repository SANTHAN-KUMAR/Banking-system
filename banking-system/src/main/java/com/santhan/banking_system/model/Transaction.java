package com.santhan.banking_system.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal; // Use BigDecimal for currency!
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor // Add AllArgsConstructor if you want a constructor with all fields
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Corresponds to BIGINT

    // Relationship mapping: Many Transactions belong to One Account
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false) // Matches the account_id column in your transactions table
    private Account account; // The account this transaction belongs to

    @Column(name = "transaction_type", length = 50)
    private String transactionType; // Corresponds to VARCHAR(50) (e.g., "deposit", "withdrawal", "transfer")

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount; // Corresponds to DECIMAL

    @Column(name = "transaction_time")
    private LocalDateTime transactionTime; // Corresponds to TIMESTAMP

    @Column(name = "description")
    private String description; // Corresponds to VARCHAR(255)

    // Relationship mapping: Optional link for transfers to the *other* account involved
    // This column (related_account_id) can be null if it's just a deposit or withdrawal
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_account_id") // Matches the related_account_id column
    private Account relatedAccount; // The account involved in a transfer (optional)

    // --- If you didn't add Lombok, add getters/setters/constructors manually ---
}
