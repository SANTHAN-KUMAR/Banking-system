package com.santhan.banking_system.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "otps", uniqueConstraints = {
        // Enforce that for a given user and purpose, there can be at most one OTP that is NOT used.
        // This requires a functional index or specific handling in older MySQL versions.
        // For simplicity with Spring Data JPA and common MySQL setups, we'll rely on the application
        // logic to invalidate old OTPs, but this constraint is ideal if your DB supports it easily.
        // A more direct unique constraint for "active" OTPs would be on (user_id, purpose)
        // if you always invalidate the old one or delete it before inserting.
        // For now, let's remove this specific unique constraint and fix the logic.
})
public class Otp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String otpCode;

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
    private boolean used = false;

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

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
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

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    @Transient // Not persisted to database
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
