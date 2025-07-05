package com.santhan.banking_system.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime; // Use java.time.LocalDateTime for modern Spring/JPA

// Spring Security imports for UserDetails
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List; // For List.of()

@Entity
@Table(name = "users") // Ensure table name is explicitly "users"
public class User implements UserDetails { // <--- Implement UserDetails

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
    private String username;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @Column(nullable = false, length = 60) // BCrypt passwords are 60 chars
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

    // --- NEW ROLE FIELD ---
    @Enumerated(EnumType.STRING) // Store enum as String in DB ('ROLE_CUSTOMER', 'ROLE_EMPLOYEE', 'ROLE_ADMIN')
    @Column(nullable = false) // Removed 'defaultValue' attribute due to compilation issue
    @NotNull(message = "User role is required")
    private UserRole role;
    // ----------------------

    @Column(name = "created_at", nullable = false, updatable = false) // Explicitly map to snake_case, make updatable = false
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false) // Explicitly map to snake_case
    private LocalDateTime updatedAt;

    // --- Constructors ---
    public User() {
        // Default constructor required by JPA
    }

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = UserRole.ROLE_CUSTOMER; // Set default role for new users
        // createdAt and updatedAt will be set by @PrePersist
    }

    // --- Getters and Setters ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // --- Getter and Setter for Role ---
    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
    // ----------------------------------

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

    // --- Lifecycle Callbacks for Auditing ---
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        // Ensure role is set on creation if somehow not set in constructor (e.g., if using default constructor + setters)
        if (this.role == null) {
            this.role = UserRole.ROLE_CUSTOMER;
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
        // Spring Security expects roles as GrantedAuthority objects
        // We create a list containing a SimpleGrantedAuthority for the user's role
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        // For simplicity, we assume accounts never expire. In a real app, this might be a boolean field.
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // For simplicity, we assume accounts are never locked. In a real app, this might be a boolean field.
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // For simplicity, we assume credentials never expire. In a real app, this might be a boolean field.
        return true;
    }

    @Override
    public boolean isEnabled() {
        // For simplicity, we assume accounts are always enabled. In a real app, this might be a boolean field.
        return true;
    }
    // ---------------------------------------------

    // Optional: toString for better debugging
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", password='[PROTECTED]'" + // Don't print password directly!
                ", role=" + role + // Include role in toString
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}