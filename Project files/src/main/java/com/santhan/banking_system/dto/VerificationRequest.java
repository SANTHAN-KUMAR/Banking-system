package com.santhan.banking_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class VerificationRequest {

    @NotBlank(message = "User identifier (username or email) cannot be empty")
    private String userIdentifier; // Can be username or email

    @NotBlank(message = "OTP code cannot be empty")
    @Pattern(regexp = "\\d{6}", message = "OTP must be a 6-digit number")
    private String otpCode;

    @NotBlank(message = "Verification purpose cannot be empty")
    private String purpose; // e.g., "EMAIL_VERIFICATION", "MOBILE_VERIFICATION", "LOGIN"

    // Getters and Setters
    public String getUserIdentifier() {
        return userIdentifier;
    }

    public void setUserIdentifier(String userIdentifier) {
        this.userIdentifier = userIdentifier;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
}
