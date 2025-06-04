package com.santhan.banking_system.service;

import com.santhan.banking_system.model.User;
import com.santhan.banking_system.model.Account; // Import Account model
import com.santhan.banking_system.repository.UserRepository;
import com.santhan.banking_system.repository.AccountRepository; // Import AccountRepository for deleting accounts
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional; // Import Transactional

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountService accountService; // Inject AccountService to delete accounts

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, AccountService accountService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountService = accountService; // Initialize AccountService
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional // Ensure user creation is transactional
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

        User savedUser = userRepository.save(user);
        System.out.println("DEBUG: User saved to database. Generated ID: " + savedUser.getId());
        return savedUser;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));
    }

    @Transactional // Ensure user update is transactional
    public User updateUser(Long id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        // Update fields (excluding password, which should be handled separately for security)
        user.setUsername(userDetails.getUsername());
        user.setEmail(userDetails.getEmail());

        // FIX: Removed .isEmpty() check as getRole() likely returns an enum (UserRole)
        // We only check if the role is not null. Validation for valid enum values
        // should ideally happen at the controller or DTO level if the input is a String.
        if (userDetails.getRole() != null) {
            user.setRole(userDetails.getRole());
        }
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Transactional // Crucial: Make the delete operation transactional
    public void deleteUser(Long id) {
        User userToDelete = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        // --- CRUCIAL FIX: Delete all associated accounts first ---
        List<Account> userAccounts = accountService.getAccountsByUserId(id); // Use the AccountService
        if (userAccounts != null && !userAccounts.isEmpty()) {
            for (Account account : userAccounts) {
                // You might want to add more robust error handling here,
                // or a specific method in AccountService to handle cascade deletion if needed.
                // For now, we'll use the existing deleteAccount method.
                accountService.deleteAccount(account.getId());
            }
            System.out.println("DEBUG: Deleted " + userAccounts.size() + " accounts for user ID: " + id);
        }
        // --- End of CRUCIAL FIX ---

        userRepository.deleteById(id);
        System.out.println("DEBUG: User with ID: " + id + " deleted successfully.");
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("DEBUG: loadUserByUsername called for username: '" + username + "'");
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
        System.out.println("DEBUG: Found user '" + user.getUsername() + "' for login with role: " + user.getRole());

        return user;
    }
}
