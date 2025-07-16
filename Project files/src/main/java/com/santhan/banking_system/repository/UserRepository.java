package com.santhan.banking_system.repository;

import com.santhan.banking_system.model.KycStatus; // Make sure this is imported
import com.santhan.banking_system.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email); // Assuming this method exists for duplicate email check
    Optional<User> findByMobileNumber(String mobileNumber); // Make sure this exists
    // This is the new method that must be in UserRepository for UserService to compile
    List<User> findByKycStatus(KycStatus kycStatus);
}
