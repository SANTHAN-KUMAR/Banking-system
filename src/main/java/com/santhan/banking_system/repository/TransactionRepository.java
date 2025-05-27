package com.santhan.banking_system.repository;

import com.santhan.banking_system.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository // This annotation is optional for interfaces extending JpaRepository, but good practice for clarity
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Custom query method: find all transactions related to a specific account
    // This will find transactions where the given account is either the source or the destination.
    List<Transaction> findBySourceAccount_IdOrDestinationAccount_Id(Long sourceAccountId, Long destinationAccountId);

    // Another useful query: find transactions by a specific type (e.g., all DEPOSITs)
    // List<Transaction> findByTransactionType(TransactionType type);

    // You can add more complex queries as needed later, e.g., by date range, by amount, etc.
}