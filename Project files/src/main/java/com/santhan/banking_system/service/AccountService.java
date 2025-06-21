package com.santhan.banking_system.service;

import com.santhan.banking_system.model.Account;
import com.santhan.banking_system.model.AccountType;
import com.santhan.banking_system.model.User;
import com.santhan.banking_system.model.Transaction; // Import Transaction model
import com.santhan.banking_system.repository.AccountRepository;
import com.santhan.banking_system.repository.UserRepository;
import com.santhan.banking_system.repository.TransactionRepository; // Import TransactionRepository
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
    private final TransactionRepository transactionRepository; // Inject TransactionRepository

    @Autowired
    public AccountService(AccountRepository accountRepository, UserRepository userRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository; // Initialize TransactionRepository
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

        // Update the user/owner if it's different and a valid user is provided
        if (updatedAccount.getUser() != null && updatedAccount.getUser().getId() != null) {
            User newOwner = userRepository.findById(updatedAccount.getUser().getId())
                    .orElseThrow(() -> new IllegalArgumentException("New owner user not found with ID: " + updatedAccount.getUser().getId()));
            existingAccount.setUser(newOwner);
        }


        return accountRepository.save(existingAccount);
    }

    @Transactional // Crucial: Make the delete operation transactional
    public void deleteAccount(Long id) {
        Account accountToDelete = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + id));

        // --- CRUCIAL FIX: Delete all associated transactions first ---
        // Find transactions where this account is either the source or destination
        List<Transaction> relatedTransactions = transactionRepository.findBySourceAccountOrDestinationAccount(accountToDelete, accountToDelete);

        if (relatedTransactions != null && !relatedTransactions.isEmpty()) {
            transactionRepository.deleteAll(relatedTransactions); // Delete all related transactions
            System.out.println("DEBUG: Deleted " + relatedTransactions.size() + " transactions for account ID: " + id);
        }
        // --- End of CRUCIAL FIX ---

        accountRepository.deleteById(id);
        System.out.println("DEBUG: Account with ID: " + id + " deleted successfully.");
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
