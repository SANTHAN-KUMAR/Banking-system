package com.santhan.banking_system.model;

import jakarta.persistence.*;
import java.time.LocalDateTime; // Use java.time.LocalDateTime for modern Spring/JPA
import java.time.LocalDate; // NEW: Import LocalDate for dateOfBirth

// Spring Security imports for UserDetails
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users") // Ensure table name is explicitly "users"
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false, length = 60) // BCrypt passwords are 60 chars
    private String password;

    @Enumerated(EnumType.STRING) // Store enum as String in DB ('ROLE_CUSTOMER', 'ROLE_EMPLOYEE', 'ROLE_ADMIN')
    @Column(nullable = false)
    private UserRole role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // --- NEW KYC Fields ---
    @Enumerated(EnumType.STRING) // Store enum as String (e.g., "PENDING", "VERIFIED")
    @Column(nullable = false)
    private KycStatus kycStatus = KycStatus.PENDING; // Default to PENDING for new users

    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth; // Date of birth (e.g., 1990-01-15)
    @Column(columnDefinition = "TEXT") // Use TEXT for potentially longer addresses
    private String address;
    @Column(unique = true) // National ID should ideally be unique
    private String nationalIdNumber; // e.g., Aadhaar, SSN, Passport Number
    private String documentType;     // e.g., "Aadhaar Card", "Passport", "Driver's License"
    private LocalDateTime kycSubmissionDate; // When KYC data was submitted
    private LocalDateTime kycVerifiedDate;   // When KYC was verified/rejected
    // --- End NEW KYC Fields ---

    // --- Constructors ---
    public User() {
        // Default constructor required by JPA
        this.kycStatus = KycStatus.PENDING; // Ensure default KYC status is set
    }

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = UserRole.ROLE_CUSTOMER; // Set default role for new users
        this.kycStatus = KycStatus.PENDING; // Set default KYC status for new users
        // createdAt and updatedAt will be set by @PrePersist
    }

    // --- Getters and Setters ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override // From UserDetails
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

    @Override // From UserDetails
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    // --- NEW KYC Getters and Setters ---
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

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
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
    // --- End NEW KYC Getters and Setters ---


    // --- Lifecycle Callbacks for Auditing ---
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.role == null) { // Ensure role is set if not by constructor
            this.role = UserRole.ROLE_CUSTOMER;
        }
        if (this.kycStatus == null) { // Ensure KYC status is set if not by constructor
            this.kycStatus = KycStatus.PENDING;
        }
        this.kycSubmissionDate = LocalDateTime.now(); // Set submission date on first persist
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
    public boolean isEnabled() { return true; }
    // ---------------------------------------------

    // Optional: toString for better debugging
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", password='[PROTECTED]'" +
                ", role=" + role +
                ", kycStatus=" + kycStatus + // Include kycStatus in toString
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
                '}';
    }
}
