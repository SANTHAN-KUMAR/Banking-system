package com.santhan.banking_system.service;

import com.santhan.banking_system.model.User;
import com.santhan.banking_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(User user) {
        System.out.println("DEBUG: UserService.createUser method called.");
        System.out.println("DEBUG: User details received by service BEFORE encoding:");
        System.out.println("DEBUG:   Username: '" + user.getUsername() + "'");
        System.out.println("DEBUG:   Email:    '" + user.getEmail() + "'");
        System.out.println("DEBUG:   Password: '" + user.getPassword() + "'"); // This should be the raw password from the form

        // Check if username or email already exists to prevent duplicates
        userRepository.findByUsername(user.getUsername()).ifPresent(u -> {
            System.err.println("ERROR: Username '" + user.getUsername() + "' already exists.");
            throw new IllegalArgumentException("Username already exists.");
        });
        userRepository.findByEmail(user.getEmail()).ifPresent(u -> {
            System.err.println("ERROR: Email '" + user.getEmail() + "' already exists.");
            throw new IllegalArgumentException("Email already exists.");
        });

        // IMPORTANT: Encode the password before saving it to the database
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        System.out.println("DEBUG: Password after encoding: '" + encodedPassword + "'");

        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        System.out.println("DEBUG: User saved to database. Generated ID: " + savedUser.getId());
        return savedUser;
    }

    // Existing method: Get all users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Existing method: Get user by ID
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));
    }

    // Existing method: Update user details (Note: password is not updated here for simplicity,
    // a separate method or careful handling would be needed if allowed)
    public User updateUser(Long id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        // Update fields (excluding password, which should be handled separately for security)
        user.setUsername(userDetails.getUsername());
        user.setEmail(userDetails.getEmail());
        // Do NOT update password here unless explicitly handled with re-encoding
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    // Existing method: Delete user
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found with ID: " + id);
        }
        userRepository.deleteById(id);
    }

    // NEW: Implementation of UserDetailsService method for Spring Security
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("DEBUG: loadUserByUsername called for username: '" + username + "'");
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
        System.out.println("DEBUG: Found user '" + user.getUsername() + "' for login.");
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}