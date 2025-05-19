package com.santhan.banking_system.service;

import com.santhan.banking_system.model.Account;
import com.santhan.banking_system.model.User; // May need User entity for linking
import com.santhan.banking_system.repository.AccountRepository; // Import Account Repository
import com.santhan.banking_system.repository.UserRepository; // May need User Repository
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional; // Import Optional

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository; // Inject UserRepository as accounts are linked to users

    public AccountService(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    // --- Example method: Create an account for a user ---
    @Transactional
    public Account createAccount(Long userId, String accountType) {
        // Find the user first
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Account account = new Account();
        account.setAccountNumber(generateUniqueAccountNumber()); // You'll implement this method
        account.setUser(user); // Link the account to the user
        account.setAccountType(accountType);
        account.setBalance(BigDecimal.ZERO); // Start with zero balance
        account.setCreatedAt(java.time.LocalDateTime.now());

        // Add the account to the user's list of accounts (optional, but good for relationship consistency)
        user.getAccounts().add(account);
        userRepository.save(user); // Save the user to update the relationship

        return accountRepository.save(account); // Save the new account
    }

    // --- Example method: Perform a Deposit (Crucial Transaction Example!) ---
    @Transactional // Essential for transactional operations like deposits
    public Account deposit(String accountNumber, BigDecimal amount) {
        // Add validation here (e.g., amount > 0)

        // Find the account
        Account account = accountRepository.findByAccountNumber(accountNumber) // Requires adding findByAccountNumber to AccountRepository
                .orElseThrow(() -> new RuntimeException("Account not found with number: " + accountNumber));

        // Update the balance
        account.setBalance(account.getBalance().add(amount));

        // (Phase 2) Create and save a Transaction record here!

        // Save the updated account balance
        Account updatedAccount = accountRepository.save(account);

        // If any error happened between fetching the account and saving,
        // the @Transactional annotation ensures the balance change is rolled back!

        return updatedAccount;
    }

    // You'll add methods for withdrawal, transfer, get balance, etc.
    private String generateUniqueAccountNumber() {
        // Implement logic to generate a unique account number (e.g., random string/number check for existence)
        // This is a separate task
        return java.util.UUID.randomUUID().toString(); // Simple placeholder
    }

    // --- If you didn't add Lombok, add getters/setters/constructors manually ---
}