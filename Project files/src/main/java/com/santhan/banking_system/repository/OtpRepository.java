package com.santhan.banking_system.repository;

import com.santhan.banking_system.model.Otp;
import com.santhan.banking_system.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {
    // Find the latest valid (not expired, not used) OTP for a user and purpose
    Optional<Otp> findTopByUserAndPurposeAndExpiresAtAfterAndUsedFalseOrderByCreatedAtDesc(User user, Otp.OtpPurpose purpose, LocalDateTime now);

    // Delete all OTPs for a user and purpose (useful for cleanup or invalidating all previous)
    // Spring Data JPA can derive this query
    void deleteByUserAndPurpose(User user, Otp.OtpPurpose purpose);
}
