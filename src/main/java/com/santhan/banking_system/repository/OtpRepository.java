package com.santhan.banking_system.repository;

import com.santhan.banking_system.model.Otp;
import com.santhan.banking_system.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {

    // Finds the most recent unused, unexpired OTP for a user and purpose
    Optional<Otp> findTopByUserAndPurposeAndExpiresAtAfterAndUsedFalseOrderByCreatedAtDesc(
            User user, Otp.OtpPurpose purpose, LocalDateTime now);

    // Method to invalidate all OTHER active OTPs for a user and purpose
    @Modifying
    @Transactional
    @Query("UPDATE Otp o SET o.used = true WHERE o.user.id = :userId AND o.purpose = :purpose AND o.id != :excludeOtpId AND o.used = false AND o.expiresAt > :now")
    int invalidateOtherActiveOtps(@Param("userId") Long userId,
                                  @Param("purpose") Otp.OtpPurpose purpose,
                                  @Param("excludeOtpId") Long excludeOtpId,
                                  @Param("now") LocalDateTime now);
}
