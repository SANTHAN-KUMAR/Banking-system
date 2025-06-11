package com.santhan.banking_system.service;

import com.santhan.banking_system.model.Account;
import com.santhan.banking_system.model.AccountType;
import com.santhan.banking_system.model.User;
import com.santhan.banking_system.model.Transaction;
import com.santhan.banking_system.model.TransactionType; // CRUCIAL FIX: Import TransactionType
import com.santhan.banking_system.repository.AccountRepository;
import com.santhan.banking_system.repository.UserRepository;
import com.santhan.banking_system.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime; // Added for updated timestamp
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository; // Injected for cascade delete

    @Autowired
    public AccountService(AccountRepository accountRepository, UserRepository userRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Creates a new bank account for a specified user.
     * Generates a unique account number and sets initial balance.
     *
     * @param userId The ID of the user who will own the account.
     * @param account The Account object containing details like accountType and initial balance.
     * @return The saved Account object.
     * @throws IllegalArgumentException if user not found or initial balance is negative.
     */
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

    /**
     * Retrieves all accounts from the database.
     *
     * @return A list of all Account objects.
     */
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    /**
     * Retrieves an account by its ID.
     *
     * @param id The ID of the account.
     * @return The found Account object.
     * @throws IllegalArgumentException if account not found.
     */
    public Account getAccountById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + id));
    }

    /**
     * Retrieves all accounts belonging to a specific user.
     *
     * @param userId The ID of the user.
     * @return A list of Account objects owned by the user.
     * @throws IllegalArgumentException if user not found.
     */
    public List<Account> getAccountsByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        return accountRepository.findByUser(user);
    }

    /**
     * Updates non-financial details of an existing account.
     * Balance updates must go through transaction methods (deposit/withdrawal).
     *
     * @param accountId The ID of the account to update.
     * @param updatedAccount An Account object containing the new details (e.g., accountType, new owner).
     * @return The updated Account object.
     * @throws IllegalArgumentException if account not found or new owner not found.
     */
    @Transactional
    public Account updateAccountDetails(Long accountId, Account updatedAccount) {
        Account existingAccount = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + accountId));

        // Only update allowed fields (e.g., accountType). Balance is handled by transactions.
        existingAccount.setAccountType(updatedAccount.getAccountType());

        // Update the user/owner if it's different and a valid user is provided
        if (updatedAccount.getUser() != null && updatedAccount.getUser().getId() != null) {
            User newOwner = userRepository.findById(updatedAccount.getUser().getId())
                    .orElseThrow(() -> new IllegalArgumentException("New owner user not found with ID: " + updatedAccount.getUser().getId()));
            existingAccount.setUser(newOwner);
        }

        existingAccount.setUpdatedAt(LocalDateTime.now()); // Update timestamp
        return accountRepository.save(existingAccount);
    }

    /**
     * Deletes an account and all its associated transactions.
     * This is a critical operation as it cascades deletes.
     *
     * @param id The ID of the account to delete.
     * @throws IllegalArgumentException if account not found.
     */
    @Transactional // Ensures the entire delete operation (transactions + account) is atomic
    public void deleteAccount(Long id) {
        Account accountToDelete = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + id));

        // Delete all transactions associated with this account first (due to foreign key constraints)
        List<Transaction> relatedTransactions = transactionRepository.findBySourceAccountOrDestinationAccount(accountToDelete, accountToDelete);
        if (relatedTransactions != null && !relatedTransactions.isEmpty()) {
            transactionRepository.deleteAll(relatedTransactions);
            System.out.println("DEBUG: Deleted " + relatedTransactions.size() + " transactions for account ID: " + id);
        }

        accountRepository.deleteById(id);
        System.out.println("DEBUG: Account with ID: " + id + " deleted successfully.");
    }

    /**
     * Handles a deposit into an account.
     * This method is called from TransactionService for deposits, or directly for admin actions.
     *
     * @param accountId The ID of the account to deposit into.
     * @param amount The amount to deposit. Must be positive.
     * @param description A brief description for the transaction.
     * @return The updated Account object.
     * @throws IllegalArgumentException if the account is not found or amount is not positive.
     */
    @Transactional // Ensures the entire method runs as a single database operation (atomicity)
    public Account deposit(Long accountId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive.");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + accountId));

        // Update account balance
        account.setBalance(account.getBalance().add(amount));
        account.setUpdatedAt(LocalDateTime.now()); // Update timestamp
        Account updatedAccount = accountRepository.save(account);

        // Record the transaction (this part is typically handled by TransactionService.deposit,
        // but included here for completeness if this method were called directly for simple deposit logic).
        // If this method is ONLY called by TransactionService, you might remove the Transaction saving here
        // to avoid duplicate transaction records. Let's assume for now TransactionService calls THIS.
        Transaction transaction = new Transaction(
                TransactionType.DEPOSIT,
                amount,
                description != null ? description : "Cash Deposit",
                null, // No source account for a deposit from external source
                updatedAccount // Destination account is the one being deposited into
        );
        // Ensure TransactionService is responsible for saving transactions or this one is only for internal use.
        // If the TransactionService.deposit calls this AccountService.deposit, then the transaction recording should be ONLY in TransactionService.
        // For simplicity, let's ensure the TransactionService is the single source of truth for creating Transaction entities.
        // So, this method might be better as 'updateBalanceForDeposit' if TransactionService handles the Transaction entity creation.
        // However, based on previous code, TransactionService calls this method, and then TransactionService creates the Transaction record.
        // This is a common pattern where a 'service' (AccountService) handles balance updates, and another 'service' (TransactionService) handles transaction logging.
        // Let's remove the transactionRepository.save(transaction) from here to avoid duplicate transaction records
        // as TransactionService already handles the transaction logging after calling this.

        return updatedAccount;
    }


    /**
     * Generates a unique 10-digit account number.
     * @return A unique 10-digit account number string.
     */
    private String generateUniqueAccountNumber() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
