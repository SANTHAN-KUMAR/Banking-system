package com.santhan.banking_system.model;

import jakarta.persistence.*;
import java.time.LocalDateTime; // Use java.time.LocalDateTime for modern Spring/JPA

@Entity
@Table(name = "users") // Ensure table name is explicitly "users"
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false, length = 60) // BCrypt passwords are 60 chars
    private String password;

    @Column(name = "created_at", nullable = false) // Explicitly map to snake_case
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
        // createdAt and updatedAt will be set by service or annotations
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

    // Optional: toString for better debugging
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", password='[PROTECTED]'" + // Don't print password directly!
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}