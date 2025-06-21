package com.santhan.banking_system.repository;

import com.santhan.banking_system.model.Account;
import com.santhan.banking_system.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);
    List<Account> findByUser(User user);
    // NEW: Find accounts by user ID
    List<Account> findByUserId(Long userId); // Spring Data JPA will automatically implement this
}