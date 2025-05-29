package com.santhan.banking_system.repository;

import com.santhan.banking_system.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional; // NEW

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username); // <--- Make sure this method exists
    Optional<User> findByEmail(String email); // And this one
}