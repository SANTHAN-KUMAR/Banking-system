package com.santhan.banking_system.service;

import com.santhan.banking_system.model.User;
import com.santhan.banking_system.model.Account; // Import Account model
import com.santhan.banking_system.model.KycStatus; // NEW: Import KycStatus
import com.santhan.banking_system.dto.KycSubmissionDto; // NEW: Import KycSubmissionDto
import com.santhan.banking_system.repository.UserRepository;
// Removed AccountRepository import as AccountService is injected
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
// Removed SimpleGrantedAuthority as User model should implement UserDetails directly or its roles are handled otherwise
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional; // Import Transactional

import jakarta.persistence.EntityNotFoundException; // NEW: Import EntityNotFoundException for clarity

import java.time.LocalDateTime;
// Removed Collections import as not explicitly used
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
        // Removed printing raw password for security reasons
        // System.out.println("DEBUG:   Password: '" + user.getPassword() + "'"); // This should be the raw password from the form

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
        user.setPassword(encodedPassword); // Using setPassword based on your current code
        System.out.println("DEBUG: Password after encoding: '" + encodedPassword + "'");

        // Set default creation and update timestamps
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        // NEW: Set default KYC status for newly created users if not already set
        if (user.getKycStatus() == null) {
            user.setKycStatus(KycStatus.PENDING);
        }

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

        // Delete all associated accounts first using AccountService
        List<Account> userAccounts = accountService.getAccountsByUserId(id);
        if (userAccounts != null && !userAccounts.isEmpty()) {
            for (Account account : userAccounts) {
                accountService.deleteAccount(account.getId());
            }
            System.out.println("DEBUG: Deleted " + userAccounts.size() + " accounts for user ID: " + id);
        }

        userRepository.deleteById(id);
        System.out.println("DEBUG: User with ID: " + id + " deleted successfully.");
    }

    // NEW METHOD FOR KYC SUBMISSION
    @Transactional // Ensures the entire method runs as a single transaction
    public User submitKycDetails(Long userId, KycSubmissionDto kycDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        // Update user's KYC fields from the DTO
        user.setFirstName(kycDto.getFirstName());
        user.setLastName(kycDto.getLastName());
        user.setDateOfBirth(kycDto.getDateOfBirth());
        user.setAddress(kycDto.getAddress());
        user.setNationalIdNumber(kycDto.getNationalIdNumber());
        user.setDocumentType(kycDto.getDocumentType());

        // Set KYC status to PENDING upon any submission/re-submission
        user.setKycStatus(KycStatus.PENDING);
        user.setKycSubmissionDate(LocalDateTime.now()); // Record when KYC was submitted

        // kycVerifiedDate will be set by an admin action later

        return userRepository.save(user); // Save the updated user entity
    }

    // NEW METHOD ADDED FOR ADMIN KYC REVIEW
    public List<User> getUsersByKycStatus(KycStatus status) {
        return userRepository.findByKycStatus(status);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("DEBUG: loadUserByUsername called for username: '" + username + "'");
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
        System.out.println("DEBUG: Found user '" + user.getUsername() + "' for login with role: " + user.getRole());

        // Assuming your 'User' entity directly implements Spring Security's UserDetails interface,
        // or you have adapted it to do so.
        return user;
    }
}
