package com.santhan.banking_system.model;

public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER,
    // New reversal types for clear audit trail
    DEPOSIT_REVERSAL,
    WITHDRAWAL_REVERSAL,
    TRANSFER_REVERSAL;

    // Optional: A method to get a more readable name for display
    public String getDisplayName() {
        return this.name().replace("_", " ");
    }
}
