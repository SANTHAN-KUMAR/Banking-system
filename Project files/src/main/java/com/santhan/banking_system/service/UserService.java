package com.santhan.banking_system.service;

import com.santhan.banking_system.model.User;
import com.santhan.banking_system.model.Account;
import com.santhan.banking_system.model.KycStatus;
import com.santhan.banking_system.model.UserRole;
import com.santhan.banking_system.model.Otp;
import com.santhan.banking_system.dto.KycSubmissionDto;
import com.santhan.banking_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountService accountService;
    private final OtpService otpService;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, AccountService accountService, OtpService otpService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountService = accountService;
        this.otpService = otpService;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByMobileNumber(String mobileNumber) {
        return userRepository.findByMobileNumber(mobileNumber);
    }

    @Transactional
    public User createUser(User user) {
        System.out.println("DEBUG: UserService.createUser method called.");
        System.out.println("DEBUG: User details received by service BEFORE encoding:");
        System.out.println("DEBUG:   Username: '" + user.getUsername() + "'");
        System.out.println("DEBUG:   Email:    '" + user.getEmail() + "'");

        // Check for existing users with the same username, email, or mobile
        userRepository.findByUsername(user.getUsername()).ifPresent(u -> {
            throw new IllegalArgumentException("Username already exists.");
        });

        userRepository.findByEmail(user.getEmail()).ifPresent(u -> {
            throw new IllegalArgumentException("Email already exists.");
        });

        if (user.getMobileNumber() != null && !user.getMobileNumber().trim().isEmpty()) {
            userRepository.findByMobileNumber(user.getMobileNumber()).ifPresent(u -> {
                throw new IllegalArgumentException("Mobile number already registered.");
            });
        }

        // Encode password and set up user properties
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        System.out.println("DEBUG: Password after encoding: '" + encodedPassword + "'");

        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        if (user.getKycStatus() == null) {
            user.setKycStatus(KycStatus.PENDING);
        }

        user.setEmailVerified(false);
        user.setMobileVerified(false);
        if (user.getRole() == null) {
            user.setRole(UserRole.ROLE_CUSTOMER);
        }

        // Save the user first - separate this from OTP generation
        User savedUser = userRepository.save(user);
        System.out.println("DEBUG: User saved to database. Generated ID: " + savedUser.getId());

        // Now handle OTP generation in a separate try/catch block to prevent user creation rollback
        try {
            otpService.generateAndSendOtp(savedUser, Otp.OtpPurpose.EMAIL_VERIFICATION);
            System.out.println("DEBUG: Email verification OTP sent for user: " + savedUser.getUsername());
        } catch (Exception e) {
            // Log but don't re-throw - we still want user creation to succeed
            System.err.println("ERROR: Failed to send OTP email for user " + savedUser.getUsername() + ": " + e.getMessage());
            e.printStackTrace();
        }

        return savedUser;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));
    }

    @Transactional
    public User updateUser(Long id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        if (!user.getUsername().equals(userDetails.getUsername())) {
            userRepository.findByUsername(userDetails.getUsername()).ifPresent(u -> {
                if (!u.getId().equals(id)) {
                    throw new IllegalArgumentException("Username '" + userDetails.getUsername() + "' already exists for another user.");
                }
            });
            user.setUsername(userDetails.getUsername());
        }

        if (userDetails.getEmail() != null && !user.getEmail().equalsIgnoreCase(userDetails.getEmail())) {
            userRepository.findByEmail(userDetails.getEmail()).ifPresent(u -> {
                if (!u.getId().equals(id)) {
                    throw new IllegalArgumentException("Email '" + userDetails.getEmail() + "' already exists for another user.");
                }
            });
            user.setEmail(userDetails.getEmail());
            user.setEmailVerified(false);
        } else if (user.getEmail() == null && userDetails.getEmail() != null) {
            userRepository.findByEmail(userDetails.getEmail()).ifPresent(u -> {
                if (!u.getId().equals(id)) {
                    throw new IllegalArgumentException("Email '" + userDetails.getEmail() + "' already exists for another user.");
                }
            });
            user.setEmail(userDetails.getEmail());
            user.setEmailVerified(false);
        } else if (userDetails.getEmail() == null && user.getEmail() != null) {
            user.setEmail(null);
            user.setEmailVerified(false);
        }


        if (userDetails.getMobileNumber() != null && !userDetails.getMobileNumber().trim().isEmpty() &&
                !user.getMobileNumber().equals(userDetails.getMobileNumber())) {
            userRepository.findByMobileNumber(userDetails.getMobileNumber()).ifPresent(u -> {
                if (!u.getId().equals(id)) {
                    throw new IllegalArgumentException("Mobile number '" + userDetails.getMobileNumber() + "' already exists for another user.");
                }
            });
            user.setMobileNumber(userDetails.getMobileNumber());
            user.setMobileVerified(false);
        } else if (user.getMobileNumber() == null && userDetails.getMobileNumber() != null && !userDetails.getMobileNumber().trim().isEmpty()) {
            userRepository.findByMobileNumber(userDetails.getMobileNumber()).ifPresent(u -> {
                if (!u.getId().equals(id)) {
                    throw new IllegalArgumentException("Mobile number '" + userDetails.getMobileNumber() + "' already exists for another user.");
                }
            });
            user.setMobileNumber(userDetails.getMobileNumber());
            user.setMobileVerified(false);
        } else if (userDetails.getMobileNumber() == null && user.getMobileNumber() != null) {
            user.setMobileNumber(null);
            user.setMobileVerified(false);
        }


        if (userDetails.getRole() != null) {
            user.setRole(userDetails.getRole());
        }
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Transactional
    public User updateUserProfile(Long userId, User updatedProfileDetails) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        if (updatedProfileDetails.getEmail() != null && !existingUser.getEmail().equalsIgnoreCase(updatedProfileDetails.getEmail())) {
            userRepository.findByEmail(updatedProfileDetails.getEmail()).ifPresent(u -> {
                if (!u.getId().equals(userId)) {
                    throw new IllegalArgumentException("Email '" + updatedProfileDetails.getEmail() + "' is already registered by another user.");
                }
            });
            existingUser.setEmail(updatedProfileDetails.getEmail());
            existingUser.setEmailVerified(false);
        } else if (existingUser.getEmail() == null && updatedProfileDetails.getEmail() != null) {
            userRepository.findByEmail(updatedProfileDetails.getEmail()).ifPresent(u -> {
                if (!u.getId().equals(userId)) {
                    throw new IllegalArgumentException("Email '" + updatedProfileDetails.getEmail() + "' is already registered by another user.");
                }
            });
            existingUser.setEmail(updatedProfileDetails.getEmail());
            existingUser.setEmailVerified(false);
        } else if (updatedProfileDetails.getEmail() == null && existingUser.getEmail() != null) {
            existingUser.setEmail(null);
            existingUser.setEmailVerified(false);
        }

        if (updatedProfileDetails.getMobileNumber() != null && !updatedProfileDetails.getMobileNumber().trim().isEmpty() &&
                !existingUser.getMobileNumber().equals(updatedProfileDetails.getMobileNumber())) {
            userRepository.findByMobileNumber(updatedProfileDetails.getMobileNumber()).ifPresent(u -> {
                if (!u.getId().equals(userId)) {
                    throw new IllegalArgumentException("Mobile number '" + updatedProfileDetails.getMobileNumber() + "' is already registered by another user.");
                }
            });
            existingUser.setMobileNumber(updatedProfileDetails.getMobileNumber());
            existingUser.setMobileVerified(false);
        } else if (existingUser.getMobileNumber() == null && updatedProfileDetails.getMobileNumber() != null && !updatedProfileDetails.getMobileNumber().trim().isEmpty()) {
            userRepository.findByMobileNumber(updatedProfileDetails.getMobileNumber()).ifPresent(u -> {
                if (!u.getId().equals(userId)) {
                    throw new IllegalArgumentException("Mobile number '" + updatedProfileDetails.getMobileNumber() + "' is already registered by another user.");
                }
            });
            existingUser.setMobileNumber(updatedProfileDetails.getMobileNumber());
            existingUser.setMobileVerified(false);
        } else if (updatedProfileDetails.getMobileNumber() == null && existingUser.getMobileNumber() != null) {
            existingUser.setMobileNumber(null);
            existingUser.setMobileVerified(false);
        }


        if (updatedProfileDetails.getFirstName() != null) {
            existingUser.setFirstName(updatedProfileDetails.getFirstName());
        }
        if (updatedProfileDetails.getLastName() != null) {
            existingUser.setLastName(updatedProfileDetails.getLastName());
        }
        if (updatedProfileDetails.getDateOfBirth() != null) {
            existingUser.setDateOfBirth(updatedProfileDetails.getDateOfBirth());
        }
        if (updatedProfileDetails.getAddress() != null) {
            existingUser.setAddress(updatedProfileDetails.getAddress());
        }
        if (updatedProfileDetails.getNationalIdNumber() != null) {
            existingUser.setNationalIdNumber(updatedProfileDetails.getNationalIdNumber());
        }
        if (updatedProfileDetails.getDocumentType() != null) {
            existingUser.setDocumentType(updatedProfileDetails.getDocumentType());
        }

        existingUser.setUpdatedAt(LocalDateTime.now());
        existingUser.setLastProfileUpdate(LocalDateTime.now());
        return userRepository.save(existingUser);
    }


    @Transactional
    public void deleteUser(Long id) {
        User userToDelete = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

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

    @Transactional
    public User setTransactionPin(Long userId, String newPin) {
        if (newPin == null || newPin.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction PIN cannot be empty.");
        }
        if (!newPin.matches("\\d{4,6}")) {
            throw new IllegalArgumentException("PIN must be 4 to 6 digits long and numeric.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        user.setTransactionPin(passwordEncoder.encode(newPin));
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    public boolean verifyTransactionPin(Long userId, String providedPin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        if (user.getTransactionPin() == null || providedPin == null) {
            return false;
        }

        return passwordEncoder.matches(providedPin, user.getTransactionPin());
    }

    @Transactional
    public User submitKycDetails(Long userId, KycSubmissionDto kycDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        user.setFirstName(kycDto.getFirstName());
        user.setLastName(kycDto.getLastName());
        user.setDateOfBirth(kycDto.getDateOfBirth());
        user.setAddress(kycDto.getAddress());
        user.setNationalIdNumber(kycDto.getNationalIdNumber());
        user.setDocumentType(kycDto.getDocumentType());

        user.setKycStatus(KycStatus.PENDING);
        user.setKycSubmissionDate(LocalDateTime.now());

        return userRepository.save(user);
    }

    public List<User> getUsersByKycStatus(KycStatus status) {
        return userRepository.findByKycStatus(status);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("DEBUG: loadUserByUsername called for username: '" + username + "'");
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
        System.out.println("DEBUG: Found user '" + user.getUsername() + "' for login with role: " + user.getRole());

        return user;
    }

    @Transactional
    public boolean verifyUserEmail(User user, String otpCode) {
        if (user == null || user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new IllegalArgumentException("User or user email cannot be null for email verification.");
        }
        boolean isOtpValid = otpService.verifyOtp(user, otpCode, Otp.OtpPurpose.EMAIL_VERIFICATION);
        if (isOtpValid) {
            user.setEmailVerified(true);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            System.out.println("User email verified successfully for: " + user.getUsername());
            return true;
        }
        System.out.println("Email verification failed for user: " + user.getUsername() + ". Invalid OTP or expired.");
        return false;
    }

    @Transactional
    public boolean verifyUserMobile(User user, String otpCode) {
        if (user == null || user.getMobileNumber() == null || user.getMobileNumber().isEmpty()) {
            throw new IllegalArgumentException("User or user mobile number cannot be null for mobile verification.");
        }
        boolean isOtpValid = otpService.verifyOtp(user, otpCode, Otp.OtpPurpose.MOBILE_VERIFICATION);
        if (isOtpValid) {
            user.setMobileVerified(true);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            return true;
        }
        System.out.println("Mobile verification failed for user: " + user.getUsername() + ". Invalid OTP or expired.");
        return false;
    }

    @Transactional
    public void resendEmailVerificationOtp(User user) {
        if (user == null || user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new IllegalArgumentException("User or user email cannot be null for resending email verification.");
        }
        if (user.isEmailVerified()) {
            throw new IllegalStateException("Email is already verified for user: " + user.getUsername());
        }
        otpService.generateAndSendOtp(user, Otp.OtpPurpose.EMAIL_VERIFICATION);
        System.out.println("Resent email verification OTP for user: " + user.getUsername());
    }

    @Transactional
    public void resendMobileVerificationOtp(User user) {
        if (user == null || user.getMobileNumber() == null || user.getMobileNumber().isEmpty()) {
            throw new IllegalArgumentException("User or user mobile number cannot be null for resending mobile verification.");
        }
        if (user.isMobileVerified()) {
            throw new IllegalStateException("Mobile number is already verified for user: " + user.getUsername());
        }
        otpService.generateAndSendOtp(user, Otp.OtpPurpose.MOBILE_VERIFICATION);
        System.out.println("Resent mobile verification OTP for user: " + user.getUsername());
    }
}
