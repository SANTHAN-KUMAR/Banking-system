package com.santhan.banking_system.repository;

import com.santhan.banking_system.model.Transaction; // Import your Transaction entity
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    // Provides standard CRUD for Transaction

    // You will likely add a method to find transactions for a specific account:
    // List<Transaction> findByAccount_Id(Long accountId); // Find by account ID
    // List<Transaction> findByAccount(Account account); // Find by Account object
    // List<Transaction> findByAccountIdOrderByTransactionTimeDesc(Long accountId); // Find by account ID, ordered by time descending
}