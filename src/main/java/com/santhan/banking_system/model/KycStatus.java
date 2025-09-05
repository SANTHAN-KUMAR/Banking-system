package com.santhan.banking_system.model;

public enum KycStatus {
    PENDING("Pending Review"),
    VERIFIED("Verified"),
    REJECTED("Rejected"),
    REQUIRES_RESUBMISSION("Requires Resubmission");

    private final String displayName;

    KycStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
