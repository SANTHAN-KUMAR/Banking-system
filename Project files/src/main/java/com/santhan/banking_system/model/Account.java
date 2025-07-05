package com.santhan.banking_system.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    @NotBlank(message = "Account number is required")
    @Size(min = 10, max = 20, message = "Account number must be between 10 and 20 characters")
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Account type is required")
    private AccountType accountType;

    @Column(nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Balance is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Balance cannot be negative")
    @Digits(integer = 15, fraction = 4, message = "Balance format is invalid")
    private BigDecimal balance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "Account must be associated with a user")
    private User user;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Custom constructor for creation (without ID and dates initially)
    // This constructor will be used when Spring binds form data to an Account object
    public Account(String accountNumber, AccountType accountType, BigDecimal balance, User user) {
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.balance = balance;
        this.user = user;
    }

    // Lifecycle callbacks for auditing fields
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