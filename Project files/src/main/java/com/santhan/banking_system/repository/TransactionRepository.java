package com.santhan.banking_system.repository;

import com.santhan.banking_system.model.Transaction;
import com.santhan.banking_system.model.Account; // Import Account model
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    // Method to find all transactions for a given account (either as source or destination)
    List<Transaction> findBySourceAccountOrDestinationAccount(Account sourceAccount, Account destinationAccount);

    // You likely already have this from Sprint 2:
    List<Transaction> findBySourceAccountId(Long accountId);
    List<Transaction> findByDestinationAccountId(Long accountId);

    // FIX: Add this method to resolve the error in TransactionService
    List<Transaction> findBySourceAccount_IdOrDestinationAccount_Id(Long sourceAccountId, Long destinationAccountId);
}
