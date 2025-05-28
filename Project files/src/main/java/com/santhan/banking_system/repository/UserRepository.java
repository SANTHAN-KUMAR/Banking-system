package com.santhan.banking_system.repository;

import com.santhan.banking_system.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional; // NEW

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username); // NEW
    Optional<User> findByEmail(String email); // NEW
}