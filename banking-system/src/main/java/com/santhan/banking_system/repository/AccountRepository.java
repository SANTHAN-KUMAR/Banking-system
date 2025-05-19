package com.santhan.banking_system.repository;

import com.santhan.banking_system.model.Account; // Import your Account entity
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional; // Import Optional if not already there

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    // Provides standard CRUD for Account

    // You will likely add a method to find an account by its account number:
    Optional<Account> findByAccountNumber(String accountNumber);
}
