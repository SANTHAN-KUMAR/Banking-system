package com.santhan.banking_system.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalDate; // Corrected: Use LocalDate for dateOfBirth

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

// Import top-level enums
import com.santhan.banking_system.model.UserRole;
import com.santhan.banking_system.model.KycStatus;

@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false, length = 60)
    private String password;

    // NEW FIELD for Transaction PIN (will store hashed value)
    @Column(name = "transaction_pin", length = 60) // BCrypt hash length
    private String transactionPin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role; // Corrected: Use top-level UserRole enum

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_profile_update")
    private LocalDateTime lastProfileUpdate;

    // NEW FIELDS for 2FA/Verification
    @Column(nullable = false)
    private boolean emailVerified = false; // Default to false

    @Column(nullable = false)
    private boolean mobileVerified = false; // Default to false

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus kycStatus = KycStatus.PENDING; // Corrected: Use top-level KycStatus

    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth; // Corrected: Back to LocalDate
    @Column(columnDefinition = "TEXT")
    private String address;
    @Column(unique = true)
    private String nationalIdNumber;
    private String documentType;
    private LocalDateTime kycSubmissionDate;
    private LocalDateTime kycVerifiedDate;

    // NEW FIELD: Mobile Number
    @Column(nullable = true, unique = true)
    private String mobileNumber;


    public User() {
        this.kycStatus = KycStatus.PENDING;
        this.emailVerified = false;
        this.mobileVerified = false;
    }

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = UserRole.ROLE_CUSTOMER;
        this.kycStatus = KycStatus.PENDING;
        this.emailVerified = false;
        this.mobileVerified = false;
    }

    // --- Getters and Setters ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTransactionPin() {
        return transactionPin;
    }

    public void setTransactionPin(String transactionPin) {
        this.transactionPin = transactionPin;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getLastProfileUpdate() {
        return lastProfileUpdate;
    }

    public void setLastProfileUpdate(LocalDateTime lastProfileUpdate) {
        this.lastProfileUpdate = lastProfileUpdate;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public boolean isMobileVerified() {
        return mobileVerified;
    }

    public void setMobileVerified(boolean mobileVerified) {
        this.mobileVerified = mobileVerified;
    }

    public KycStatus getKycStatus() {
        return kycStatus;
    }

    public void setKycStatus(KycStatus kycStatus) {
        this.kycStatus = kycStatus;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getDateOfBirth() { // Corrected: Return LocalDate
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) { // Corrected: Accept LocalDate
        this.dateOfBirth = dateOfBirth;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getNationalIdNumber() {
        return nationalIdNumber;
    }

    public void setNationalIdNumber(String nationalIdNumber) {
        this.nationalIdNumber = nationalIdNumber;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public LocalDateTime getKycSubmissionDate() {
        return kycSubmissionDate;
    }

    public void setKycSubmissionDate(LocalDateTime kycSubmissionDate) {
        this.kycSubmissionDate = kycSubmissionDate;
    }

    public LocalDateTime getKycVerifiedDate() {
        return kycVerifiedDate;
    }

    public void setKycVerifiedDate(LocalDateTime kycVerifiedDate) {
        this.kycVerifiedDate = kycVerifiedDate;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    // --- Lifecycle Callbacks for Auditing ---
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.role == null) {
            this.role = UserRole.ROLE_CUSTOMER;
        }
        if (this.kycStatus == null) {
            this.kycStatus = KycStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    // ----------------------------------------

    // --- UserDetails interface implementations ---
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        // User is enabled only if their email is verified
        return this.emailVerified;
    }
    // ---------------------------------------------

    // Optional: toString for better debugging
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", password='[PROTECTED]'" +
                ", transactionPin='[PROTECTED]'" +
                ", role=" + role +
                ", kycStatus=" + kycStatus +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", address='" + address + '\'' +
                ", nationalIdNumber='" + nationalIdNumber + '\'' +
                ", documentType='" + documentType + '\'' +
                ", kycSubmissionDate=" + kycSubmissionDate +
                ", kycVerifiedDate=" + kycVerifiedDate +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", lastProfileUpdate=" + lastProfileUpdate +
                ", mobileNumber='" + mobileNumber + '\'' +
                ", emailVerified=" + emailVerified +
                ", mobileVerified=" + mobileVerified +
                '}';
    }
}
