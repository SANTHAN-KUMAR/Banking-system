package com.santhan.banking_system.service;

import com.santhan.banking_system.model.Otp;
import com.santhan.banking_system.model.User;
import com.santhan.banking_system.repository.OtpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Modifying; // Import for @Modifying
import org.springframework.dao.PessimisticLockingFailureException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class OtpService {

    private final OtpRepository otpRepository;
    private final EmailService emailService;

    private static final int OTP_VALIDITY_MINUTES = 5;

    @Autowired
    public OtpService(OtpRepository otpRepository, EmailService emailService) {
        this.otpRepository = otpRepository;
        this.emailService = emailService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateAndSendOtp(User user, Otp.OtpPurpose purpose) {
        if (user == null || user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new IllegalArgumentException("User and user's email must not be null for OTP generation.");
        }

        return generateAndSendOtpWithRetry(user, purpose, 3);
    }

    private String generateAndSendOtpWithRetry(User user, Otp.OtpPurpose purpose, int maxRetries) {
        String otpCode = String.format("%06d", new Random().nextInt(999999));
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryTime = now.plusMinutes(OTP_VALIDITY_MINUTES);

        int attempts = 0;
        while (attempts < maxRetries) {
            try {
                // 1. Create and save the NEW OTP first. This is a pure insert.
                Otp newOtp = new Otp();
                newOtp.setOtpCode(otpCode);
                newOtp.setUser(user);
                newOtp.setCreatedAt(now);
                newOtp.setExpiresAt(expiryTime);
                newOtp.setPurpose(purpose);
                newOtp.setUsed(false);

                otpRepository.save(newOtp); // Save the new OTP
                System.out.println("DEBUG: New OTP saved to database. OTP Code: " + otpCode + ", User ID: " + user.getId());

                // 2. Invalidate all OTHER active OTPs for this user and purpose.
                // This is a bulk update, which can be more efficient and less prone to row-level
                // locking conflicts than selecting and updating one by one.
                // We need a custom method in OtpRepository for this.
                otpRepository.invalidateOtherActiveOtps(user.getId(), purpose, newOtp.getId(), now);
                System.out.println("DEBUG: Invalidated previous active OTPs for user " + user.getUsername() + ", purpose " + purpose);

                // Send email
                String subject = "Your Banking System OTP";
                String body = String.format("Dear %s,\n\nYour One-Time Password (OTP) for %s is: %s\n\nThis OTP is valid for %d minutes.\n\nDo not share this OTP with anyone.\n\nSincerely,\nYour Banking System",
                        user.getFirstName() != null ? user.getFirstName() : user.getUsername(),
                        purpose.name().replace("_", " ").toLowerCase(),
                        otpCode, OTP_VALIDITY_MINUTES);

                emailService.sendEmail(user.getEmail(), subject, body);
                System.out.println("DEBUG: OTP email send initiated for " + user.getEmail() + " for purpose: " + purpose);
                
                // If we reach here, the operation was successful
                return otpCode;
                
            } catch (PessimisticLockingFailureException e) {
                attempts++;
                System.err.println("WARN: Lock timeout occurred during OTP generation for user " + user.getUsername() + 
                                 " (attempt " + attempts + "/" + maxRetries + "): " + e.getMessage());
                
                if (attempts >= maxRetries) {
                    System.err.println("ERROR: Failed to generate OTP after " + maxRetries + " attempts for user " + user.getUsername());
                    throw new RuntimeException("Failed to generate OTP after " + maxRetries + " attempts due to database lock timeout. Please try again.", e);
                }
                
                // Wait before retry with exponential backoff
                try {
                    long waitTime = (long) (100 * Math.pow(2, attempts - 1)); // 100ms, 200ms, 400ms, etc.
                    System.out.println("DEBUG: Waiting " + waitTime + "ms before retry...");
                    Thread.sleep(waitTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread interrupted during OTP generation retry", ie);
                }
                
                // Generate new OTP code for retry to avoid duplicate issues
                otpCode = String.format("%06d", new Random().nextInt(999999));
                now = LocalDateTime.now();
                expiryTime = now.plusMinutes(OTP_VALIDITY_MINUTES);
                
            } catch (Exception e) {
                System.err.println("ERROR: Failed to process OTP for user " + user.getUsername() + ": " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Failed to generate and send OTP.", e);
            }
        }
        
        // This should never be reached due to the throw in the catch block
        throw new RuntimeException("Unexpected error in OTP generation retry logic");
    }

    @Transactional
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

        if (storedOtp.isExpired()) {
            storedOtp.setUsed(true);
            otpRepository.save(storedOtp);
            System.out.println("OTP verification failed: OTP expired for user " + user.getUsername());
            return false;
        }

        storedOtp.setUsed(true);
        otpRepository.save(storedOtp);
        System.out.println("OTP verified successfully for user " + user.getUsername());
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String resendOtp(User user, Otp.OtpPurpose purpose) {
        return generateAndSendOtp(user, purpose);
    }
}
