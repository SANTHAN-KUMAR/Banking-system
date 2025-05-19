package com.santhan.banking_system.service;

import com.santhan.banking_system.model.Transaction;
import com.santhan.banking_system.repository.TransactionRepository; // Import Transaction Repository
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import for transaction management

import java.util.List; // Import List

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    // --- Example method: Save a transaction ---
    @Transactional // Add @Transactional as it writes to DB
    public Transaction saveTransaction(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    // --- Example method: Get transactions for an account ---
    @Transactional(readOnly = true) // readOnly = true for reads
    public List<Transaction> getTransactionsByAccountId(Long accountId) {
        // You'll need to add a method like findByAccountId or findByAccount_Id
        // to your TransactionRepository interface first!
        // Example: return transactionRepository.findByAccountIdOrderByTransactionTimeDesc(accountId);
        return null; // Placeholder until you add the repository method
    }

    // You'll add methods to get transactions for a specific period, etc.
}