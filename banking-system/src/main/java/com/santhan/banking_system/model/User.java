package com.santhan.banking_system.model;

import jakarta.persistence.*; // Use jakarta.persistence for newer Spring Boot versions
import lombok.Data; // Import Lombok annotations if you added Lombok
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.*;
import java.time.LocalDateTime; // Use java.time for modern date/time

@Data // Lombok: Generates getters, setters, toString, equals, hashCode
@NoArgsConstructor // Lombok: Generates a no-argument constructor
@AllArgsConstructor // Lombok: Generates a constructor with all fields

@Entity // JPA: Marks this class as a database entity
@Table(name = "users") // JPA: Specifies the name of the table in the database
public class User {

    @Id // JPA: Marks this field as the primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // JPA: Configures auto-incrementing ID
    private Long id; // Corresponds to BIGINT in SQL

    @Column(name = "username", nullable = false, unique = true) // JPA: Maps to 'username' column
    private String username; // Corresponds to VARCHAR(255)

    @Column(name = "password_hash", nullable = false) // JPA: Maps to 'password_hash' column
    private String passwordHash; // Corresponds to VARCHAR(255)

    @Column(name = "email") // JPA: Maps to 'email' column
    private String email; // Corresponds to VARCHAR(255)

    @Column(name = "created_at") // JPA: Maps to 'created_at' column
    private LocalDateTime createdAt; // Corresponds to TIMESTAMP

    // This is the relationship mapping: One User can have Many Accounts
    // 'mappedBy' indicates the field in the 'Account' entity that owns the relationship (the foreign key side)
    // CascadeType.ALL: If a User is deleted, all associated Accounts will also be deleted. Be careful with this!
    // orphanRemoval = true: If an Account is removed from the 'accounts' list of a User, it's deleted from the DB.
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Account> accounts = new ArrayList<>(); // Initialize to avoid NullPointerException

    // --- If you didn't add Lombok, you would manually add getters and setters like this: ---
    // public Long getId() { return id; }
    // public void setId(Long id) { this.id = id; }
    // public String getUsername() { return username; }
    // public void setUsername(String username) { this.username = username; }
    // // ... and so on for all fields ...
    // public List<Account> getAccounts() { return accounts; }
    // public void setAccounts(List<Account> accounts) { this.accounts = accounts; }
    // --- You would also need to write constructors ---

}