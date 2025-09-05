package com.santhan.banking_system.service;

import com.santhan.banking_system.model.Otp;
import com.santhan.banking_system.model.User;
import com.santhan.banking_system.repository.OtpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private final OtpRepository otpRepository;
    private final EmailService emailService;

    // Emergency bypass system - stores OTPs in memory when DB operations fail
    // Key: userId + "_" + purpose, Value: OTP code and expiry time
    private final Map<String, EmergencyOtp> emergencyOtpMap = new ConcurrentHashMap<>();

    private static final int OTP_VALIDITY_MINUTES = 5;

    @Autowired
    public OtpService(OtpRepository otpRepository, EmailService emailService) {
        this.otpRepository = otpRepository;
        this.emailService = emailService;
    }

    /**
     * Class to represent emergency in-memory OTPs
     */
    private static class EmergencyOtp {
        private final String otpCode;
        private final LocalDateTime expiresAt;
        private boolean used;

        public EmergencyOtp(String otpCode) {
            this.otpCode = otpCode;
            this.expiresAt = LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES);
            this.used = false;
        }

        public boolean isValid() {
            return !used && LocalDateTime.now().isBefore(expiresAt);
        }

        public void markUsed() {
            this.used = true;
        }

        public String getOtpCode() {
            return otpCode;
        }
    }

    /**
     * Generate and send OTP with emergency fallback if DB operations fail
     */
    public String generateAndSendOtp(User user, Otp.OtpPurpose purpose) {
        if (user == null || user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new IllegalArgumentException("User and user's email must not be null for OTP generation.");
        }

        String otpCode = String.format("%06d", new Random().nextInt(999999));
        System.out.println("DEBUG: Generating OTP for user " + user.getUsername() + ", purpose: " + purpose);

        boolean useEmergencyMode = false;
        Exception lastException = null;

        // First try normal database operation with one retry
        try {
            return executeOtpCreation(user, purpose, otpCode);
        } catch (Exception e) {
            lastException = e;
            System.err.println("CRITICAL: Database OTP creation failed: " + e.getMessage());
            System.err.println("INFO: Activating emergency in-memory OTP system for user " + user.getUsername());
            useEmergencyMode = true;
        }

        // If database operations failed, use emergency in-memory OTP system
        if (useEmergencyMode) {
            String mapKey = user.getId() + "_" + purpose.name();

            // Store OTP in memory map
            emergencyOtpMap.put(mapKey, new EmergencyOtp(otpCode));
            System.out.println("DEBUG: Stored emergency OTP in memory for user ID: " + user.getId());

            // Send email with OTP
            sendOtpEmail(user, otpCode, purpose);

            System.out.println("NOTICE: Using emergency in-memory OTP for user: " + user.getUsername() +
                    ". Original error: " + (lastException != null ? lastException.getMessage() : "unknown"));
            return otpCode;
        }

        throw new RuntimeException("Failed to create OTP and emergency system also failed", lastException);
    }

    /**
     * The OTP creation logic via JPA/Hibernate
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected String executeOtpCreation(User user, Otp.OtpPurpose purpose, String otpCode) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryTime = now.plusMinutes(OTP_VALIDITY_MINUTES);

        // 1. Create and save the NEW OTP first
        Otp newOtp = new Otp();
        newOtp.setOtpCode(otpCode);
        newOtp.setUser(user);
        newOtp.setCreatedAt(now);
        newOtp.setExpiresAt(expiryTime);
        newOtp.setPurpose(purpose);
        newOtp.setUsed(false);

        // This is the line that can fail with lock timeout
        Otp savedOtp = otpRepository.save(newOtp);
        System.out.println("DEBUG: New OTP saved to database. OTP Code: " + otpCode + ", User ID: " + user.getId());

        // 2. Invalidate all OTHER active OTPs for this user and purpose
        int invalidatedCount = otpRepository.invalidateOtherActiveOtps(user.getId(), purpose, savedOtp.getId(), now);
        System.out.println("DEBUG: Invalidated " + invalidatedCount + " previous active OTPs for user " + user.getUsername() + ", purpose " + purpose);

        // 3. Send email with OTP
        sendOtpEmail(user, otpCode, purpose);

        return otpCode;
    }

    /**
     * Sends the OTP email
     */
    private void sendOtpEmail(User user, String otpCode, Otp.OtpPurpose purpose) {
        String subject = "Your Banking System OTP";
        String body = String.format("Dear %s,\n\nYour One-Time Password (OTP) for %s is: %s\n\nThis OTP is valid for %d minutes.\n\nDo not share this OTP with anyone.\n\nSincerely,\nYour Banking System",
                user.getFirstName() != null ? user.getFirstName() : user.getUsername(),
                purpose.name().replace("_", " ").toLowerCase(),
                otpCode, OTP_VALIDITY_MINUTES);

        try {
            emailService.sendEmail(user.getEmail(), subject, body);
            System.out.println("DEBUG: OTP email sent to " + user.getEmail() + " for purpose: " + purpose);
        } catch (Exception e) {
            System.err.println("WARNING: Failed to send OTP email to " + user.getEmail() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Verify OTP with fallback to in-memory emergency OTPs
     */
    @Transactional
    public boolean verifyOtp(User user, String providedOtp, Otp.OtpPurpose purpose) {
        if (user == null) {
            System.out.println("OTP verification failed: User is null.");
            return false;
        }

        // First try emergency in-memory OTP if one exists
        String mapKey = user.getId() + "_" + purpose.name();
        EmergencyOtp emergencyOtp = emergencyOtpMap.get(mapKey);

        if (emergencyOtp != null) {
            System.out.println("DEBUG: Found emergency in-memory OTP for user ID: " + user.getId());

            if (emergencyOtp.isValid() && providedOtp != null && emergencyOtp.getOtpCode().equals(providedOtp)) {
                // Mark as used and remove from map
                emergencyOtp.markUsed();
                emergencyOtpMap.remove(mapKey);
                System.out.println("DEBUG: Verified emergency in-memory OTP for user: " + user.getUsername());
                return true;
            } else if (!emergencyOtp.isValid()) {
                // Clean up expired emergency OTPs
                emergencyOtpMap.remove(mapKey);
                System.out.println("DEBUG: Emergency in-memory OTP expired or already used for user: " + user.getUsername());
                return false;
            }
        }

        // Fall back to database OTP verification if no valid emergency OTP exists or if emergency OTP didn't match
        try {
            Optional<Otp> storedOtpOptional = otpRepository.findTopByUserAndPurposeAndExpiresAtAfterAndUsedFalseOrderByCreatedAtDesc(
                    user, purpose, LocalDateTime.now());

            if (storedOtpOptional.isEmpty()) {
                System.out.println("OTP verification failed: No active OTP found in DB for user " + user.getUsername() + " and purpose " + purpose);
                return false;
            }

            Otp storedOtp = storedOtpOptional.get();

            if (providedOtp == null || !storedOtp.getOtpCode().equals(providedOtp)) {
                System.out.println("OTP verification failed: Provided OTP does not match DB OTP for user " + user.getUsername());
                return false;
            }

            if (storedOtp.isExpired()) {
                storedOtp.setUsed(true);
                otpRepository.save(storedOtp);
                System.out.println("OTP verification failed: DB OTP expired for user " + user.getUsername());
                return false;
            }

            storedOtp.setUsed(true);
            otpRepository.save(storedOtp);
            System.out.println("OTP verified successfully from DB for user " + user.getUsername());
            return true;
        } catch (Exception e) {
            System.err.println("ERROR: Database OTP verification failed unexpectedly for user " + user.getUsername() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String resendOtp(User user, Otp.OtpPurpose purpose) {
        // Resend also attempts to use the primary DB method first, with in-memory fallback
        return generateAndSendOtp(user, purpose);
    }
}
