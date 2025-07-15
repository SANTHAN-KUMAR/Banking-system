package com.santhan.banking_system.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "otps")
public class Otp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String otpCode; // Renamed 'code' to 'otpCode' for clarity

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpPurpose purpose;

    @Column(nullable = false)
    private boolean used = false; // NEW FIELD: To mark if OTP has been used

    public enum OtpPurpose {
        EMAIL_VERIFICATION,
        MOBILE_VERIFICATION,
        PASSWORD_RESET,
        TRANSACTION_AUTHORIZATION,
        LOGIN // For 2FA login
    }

    public Otp() {
    }

    public Otp(String otpCode, User user, LocalDateTime createdAt, LocalDateTime expiresAt, OtpPurpose purpose, boolean used) {
        this.otpCode = otpCode;
        this.user = user;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.purpose = purpose;
        this.used = used;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOtpCode() { // Renamed getter
        return otpCode;
    }

    public void setOtpCode(String otpCode) { // Renamed setter
        this.otpCode = otpCode;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public OtpPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(OtpPurpose purpose) {
        this.purpose = purpose;
    }

    public boolean isUsed() { // NEW Getter
        return used;
    }

    public void setUsed(boolean used) { // NEW Setter
        this.used = used;
    }

    // Helper method to check if OTP is expired
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
