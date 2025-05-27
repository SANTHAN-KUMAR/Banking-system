package com.santhan.banking_system.service;

import com.santhan.banking_system.model.Account;
import com.santhan.banking_system.model.AccountType; // Import AccountType
import com.santhan.banking_system.model.User;
import com.santhan.banking_system.repository.AccountRepository;
import com.santhan.banking_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Autowired
    public AccountService(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Account createAccount(Long userId, Account account) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        String newAccountNumber = generateUniqueAccountNumber();
        while (accountRepository.findByAccountNumber(newAccountNumber).isPresent()) {
            newAccountNumber = generateUniqueAccountNumber();
        }
        account.setAccountNumber(newAccountNumber);
        account.setUser(user);

        if (account.getBalance() == null || account.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            account.setBalance(BigDecimal.ZERO);
        }

        return accountRepository.save(account);
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public Account getAccountById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + id));
    }

    public List<Account> getAccountsByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        return accountRepository.findByUser(user);
    }

    @Transactional
    public Account updateAccountDetails(Long accountId, Account updatedAccount) {
        Account existingAccount = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + accountId));

        // Only update allowed fields (e.g., accountType). Balance is handled by transactions.
        existingAccount.setAccountType(updatedAccount.getAccountType());
        // If you add other non-financial fields like description, update them here:
        // existingAccount.setDescription(updatedAccount.getDescription());

        return accountRepository.save(existingAccount);
    }

    public void deleteAccount(Long id) {
        accountRepository.deleteById(id);
    }

    private String generateUniqueAccountNumber() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}