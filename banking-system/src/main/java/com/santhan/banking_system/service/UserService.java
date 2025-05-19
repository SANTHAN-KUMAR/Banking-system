package com.santhan.banking_system.service;

import com.santhan.banking_system.model.User;
import com.santhan.banking_system.repository.UserRepository; // Import the User Repository
import org.springframework.beans.factory.annotation.Autowired; // Used with field injection, less preferred
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import for transaction management

// Assuming you used Lombok's @Data, @NoArgsConstructor, @AllArgsConstructor on your entities

@Service // Marks this as a Spring Service component
public class UserService {

    private final UserRepository userRepository; // Declare the repository dependency

    // Constructor Injection: Spring automatically injects the UserRepository instance
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // --- Example method: Create a new user ---
    @Transactional // Recommended for methods that write to the database
    public User createUser(User user) {
        // Add business logic here before saving, e.g.,
        // - Validate user data (username format, email format)
        // - Hash the user's password before saving (CRITICAL SECURITY STEP!)
        // user.setPasswordHash(hashPassword(user.getPasswordHash())); // You'll implement hashPassword later

        user.setCreatedAt(java.time.LocalDateTime.now()); // Set creation time

        // Use the injected repository to save the user to the database
        return userRepository.save(user);
    }

    // --- Example method: Find a user by ID ---
    @Transactional(readOnly = true) // readOnly = true is a performance optimization for read operations
    public User getUserById(Long id) {
        // Use the injected repository to find the user
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id)); // Basic error handling
    }

    // You'll add more methods here later (e.g., findByUsername, update user, delete user)
}