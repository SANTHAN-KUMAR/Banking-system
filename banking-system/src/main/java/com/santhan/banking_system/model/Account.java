package com.santhan.banking_system.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal; // Use BigDecimal for currency!
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor // Add AllArgsConstructor if you want a constructor with all fields (including relationships)
// Often you might add a custom constructor that doesn't include the lists to avoid issues.
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Corresponds to BIGINT

    @Column(name = "account_number", nullable = false, unique = true)
    private String accountNumber; // Corresponds to VARCHAR

    // This is the relationship mapping: Many Accounts belong to One User
    // '@JoinColumn' specifies the foreign key column in THIS (accounts) table that links to the User table
    @ManyToOne(fetch = FetchType.LAZY) // FetchType.LAZY is often used to avoid loading the whole User object every time you load an Account
    @JoinColumn(name = "user_id", nullable = false) // Matches the user_id column in your accounts table
    private User user; // The Java field representing the User this account belongs to

    @Column(name = "account_type", length = 50)
    private String accountType; // Corresponds to VARCHAR(50)

    @Column(name = "balance", nullable = false, precision = 19, scale = 2) // precision and scale match your DECIMAL(19, 2)
    private BigDecimal balance = BigDecimal.ZERO; // Corresponds to DECIMAL, initialize to 0

    @Column(name = "created_at")
    private LocalDateTime createdAt; // Corresponds to TIMESTAMP

    // This is the relationship mapping: One Account can have Many Transactions
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true) // 'account' is the field name in Transaction.java
    private List<Transaction> transactions = new ArrayList<>(); // Initialize the list

    // --- If you didn't add Lombok, add getters/setters/constructors manually ---
}