package com.santhan.banking_system.service;

import com.santhan.banking_system.model.Otp;
import com.santhan.banking_system.model.User;
import com.santhan.banking_system.repository.OtpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class OtpService {

    private final OtpRepository otpRepository;
    private final EmailService emailService; // <<< Changed from JavaMailSender to EmailService

    // OTP validity period in minutes
    private static final int OTP_VALIDITY_MINUTES = 5;

    @Autowired
    public OtpService(OtpRepository otpRepository, EmailService emailService) { // <<< Changed constructor
        this.otpRepository = otpRepository;
        this.emailService = emailService;
    }

    /**
     * Generates and sends an OTP to the user's email.
     * This method runs in a NEW transaction. If it fails, the calling transaction (e.g., user creation) is not affected.
     * @param user The User object for whom the OTP is generated.
     * @param purpose The purpose of the OTP (e.g., EMAIL_VERIFICATION).
     * @return The generated OTP code.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateAndSendOtp(User user, Otp.OtpPurpose purpose) {
        if (user == null || user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new IllegalArgumentException("User and user's email must not be null for OTP generation.");
        }

        // Generate a 6-digit numeric OTP
        String otpCode = String.format("%06d", new Random().nextInt(999999));
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryTime = now.plusMinutes(OTP_VALIDITY_MINUTES);

        // Invalidate any existing active OTPs for this user and purpose
        otpRepository.findTopByUserAndPurposeAndExpiresAtAfterAndUsedFalseOrderByCreatedAtDesc(user, purpose, now)
                .ifPresent(existingOtp -> {
                    existingOtp.setUsed(true); // Mark as used/invalidated
                    otpRepository.save(existingOtp);
                });

        // Create and save new OTP
        Otp otp = new Otp();
        otp.setOtpCode(otpCode);
        otp.setUser(user);
        otp.setCreatedAt(now);
        otp.setExpiresAt(expiryTime);
        otp.setPurpose(purpose);
        otp.setUsed(false); // Mark as not used initially

        try {
            otpRepository.save(otp); // Save the new OTP to the database
            System.out.println("DEBUG: OTP saved to database. OTP Code: " + otpCode + ", User ID: " + user.getId());

            // Send email using EmailService
            String subject = "Your Banking System OTP";
            String body = String.format("Dear %s,\n\nYour One-Time Password (OTP) for %s is: %s\n\nThis OTP is valid for %d minutes.\n\nDo not share this OTP with anyone.\n\nSincerely,\nYour Banking System",
                    user.getFirstName() != null ? user.getFirstName() : user.getUsername(),
                    purpose.name().replace("_", " ").toLowerCase(),
                    otpCode, OTP_VALIDITY_MINUTES);

            emailService.sendEmail(user.getEmail(), subject, body); // <<< Use EmailService here
            System.out.println("DEBUG: OTP email send initiated for " + user.getEmail() + " for purpose: " + purpose);
        } catch (Exception e) {
            System.err.println("ERROR: Failed to process OTP for user " + user.getUsername() + ": " + e.getMessage());
            e.printStackTrace();
            // Re-throw the exception to ensure the transaction is rolled back
            throw new RuntimeException("Failed to generate and send OTP.", e);
        }

        return otpCode; // Return the generated code
    }

    /**
     * Verifies a provided OTP against the stored OTP for a specific user and purpose.
     * @param user The User object for whom the OTP is being verified.
     * @param providedOtp The OTP code provided by the user.
     * @param purpose The purpose of the OTP.
     * @return true if the OTP is valid and not expired, false otherwise.
     */
    @Transactional // This transaction ensures OTP is marked as used or deleted
    public boolean verifyOtp(User user, String providedOtp, Otp.OtpPurpose purpose) {
        if (user == null) {
            System.out.println("OTP verification failed: User is null.");
            return false;
        }

        Optional<Otp> storedOtpOptional = otpRepository.findTopByUserAndPurposeAndExpiresAtAfterAndUsedFalseOrderByCreatedAtDesc(
                user, purpose, LocalDateTime.now());

        if (storedOtpOptional.isEmpty()) {
            System.out.println("OTP verification failed: No active OTP found for user " + user.getUsername() + " and purpose " + purpose);
            return false;
        }

        Otp storedOtp = storedOtpOptional.get();

        if (providedOtp == null || !storedOtp.getOtpCode().equals(providedOtp)) {
            System.out.println("OTP verification failed: Provided OTP does not match for user " + user.getUsername());
            return false;
        }

        // Check for expiry (assuming Otp.isExpired() method exists and checks against current time)
        // If not, you might need to add: storedOtp.getExpiresAt().isBefore(LocalDateTime.now())
        if (storedOtp.isExpired()) { // Assuming Otp.isExpired()
            storedOtp.setUsed(true); // Mark as used/expired
            otpRepository.save(storedOtp);
            System.out.println("OTP verification failed: OTP expired for user " + user.getUsername());
            return false;
        }


        // OTP is valid and not expired. Mark it as used.
        storedOtp.setUsed(true); // KEY CHANGE: Mark OTP as used
        otpRepository.save(storedOtp);
        System.out.println("OTP verified successfully for user " + user.getUsername());
        return true;
    }

    /**
     * Resends an OTP to the user's email.
     * @param user The User object for whom the OTP is to be resent.
     * @param purpose The purpose of the OTP.
     * @return The newly generated OTP code.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW) // Resend also in new transaction
    public String resendOtp(User user, Otp.OtpPurpose purpose) {
        return generateAndSendOtp(user, purpose); // Simply re-use the generate and send logic
    }

    // Removed the private sendOtpEmail helper method as EmailService is used directly.
}
