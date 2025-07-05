package com.santhan.banking_system.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Data Transfer Object for transaction requests.
 * Includes validation annotations for input validation.
 */
public class TransactionRequest {

    @NotNull(message = "Account ID is required")
    @Positive(message = "Account ID must be positive")
    private Long accountId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 15, fraction = 2, message = "Amount format is invalid")
    private BigDecimal amount;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    // For transfers
    @Positive(message = "Destination account ID must be positive")
    private Long destinationAccountId;

    // Constructors
    public TransactionRequest() {}

    public TransactionRequest(Long accountId, BigDecimal amount, String description) {
        this.accountId = accountId;
        this.amount = amount;
        this.description = description;
    }

    public TransactionRequest(Long accountId, BigDecimal amount, String description, Long destinationAccountId) {
        this.accountId = accountId;
        this.amount = amount;
        this.description = description;
        this.destinationAccountId = destinationAccountId;
    }

    // Getters and Setters
    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getDestinationAccountId() {
        return destinationAccountId;
    }

    public void setDestinationAccountId(Long destinationAccountId) {
        this.destinationAccountId = destinationAccountId;
    }
}