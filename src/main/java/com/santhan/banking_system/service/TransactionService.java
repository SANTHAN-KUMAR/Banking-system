package com.santhan.banking_system.service;

import com.santhan.banking_system.model.Account;
import com.santhan.banking_system.model.Transaction;
import com.santhan.banking_system.model.TransactionType;
import com.santhan.banking_system.repository.AccountRepository;
import com.santhan.banking_system.repository.TransactionRepository;
import com.santhan.banking_system.util.HashUtil; // Assuming this class exists and contains necessary hashing methods
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final FraudAlertService fraudAlertService;
    private final EmailService emailService; // Inject EmailService

    @Autowired
    public TransactionService(AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              FraudAlertService fraudAlertService,
                              EmailService emailService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.fraudAlertService = fraudAlertService;
        this.emailService = emailService;
    }

    private Transaction createAndSaveChainedTransaction(
            TransactionType type, BigDecimal amount, String description,
            Account sourceAccount, Account destinationAccount) {

        Transaction newTransaction = new Transaction(type, amount, description, sourceAccount, destinationAccount);

        // Use the locking method to prevent race conditions
        Optional<Transaction> latestTransactionOptional = transactionRepository.findTopByOrderByTransactionDateDescIdDesc();
        String previousHash = latestTransactionOptional.map(Transaction::getTransactionHash).orElse("0");

        newTransaction.setPreviousTransactionHash(previousHash);
        newTransaction.setStatus("COMPLETED"); // Ensure status is set for new transactions
        newTransaction.setReversed(false); // Ensure new transactions are not marked reversed

        Transaction savedTransaction = transactionRepository.save(newTransaction);

        String transactionDataString = HashUtil.generateTransactionDataString(
                savedTransaction.getId(),
                savedTransaction.getTransactionType(),
                savedTransaction.getAmount(),
                savedTransaction.getDescription(),
                savedTransaction.getSourceAccount() != null ? savedTransaction.getSourceAccount().getId() : null,
                savedTransaction.getDestinationAccount() != null ? savedTransaction.getDestinationAccount().getId() : null,
                savedTransaction.getTransactionDate()
        );

        // --- START SUPER DEBUG LOGGING (for saving) ---
        System.out.println("\nDEBUG HASH INPUT FOR TRANSACTION ID " + savedTransaction.getId());
        System.out.println("------------------------------------------------------------------");
        System.out.println("Full String for Hashing: [" + transactionDataString + "]");
        System.out.println("Length of string: " + transactionDataString.length());
        for (int i = 0; i < transactionDataString.length(); i++) {
            char c = transactionDataString.charAt(i);
            System.out.println("  Index " + i + ": Char='" + c + "' (Unicode: " + (int) c + ")");
        }
        System.out.println("------------------------------------------------------------------\n");
        // --- END SUPER DEBUG LOGGING ---

        String currentHash = HashUtil.calculateSHA256Hash(transactionDataString);
        savedTransaction.setTransactionHash(currentHash);

        Transaction finalSavedTransaction = transactionRepository.save(savedTransaction); // Save again with hash

        // NEW: Evaluate the transaction for fraud after it's successfully chained and saved
        fraudAlertService.evaluateTransactionForFraud(finalSavedTransaction);

        return finalSavedTransaction;
    }

    @Transactional
    public Account deposit(Long accountId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive.");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + accountId));

        account.setBalance(account.getBalance().add(amount));
        account.setUpdatedAt(LocalDateTime.now());
        Account updatedAccount = accountRepository.save(account);

        createAndSaveChainedTransaction(
                TransactionType.DEPOSIT,
                amount,
                description != null ? description : "Cash Deposit",
                null,
                updatedAccount
        );

        // Send deposit email notification
        if (account.getUser() != null && account.getUser().getEmail() != null) {
            String userEmail = account.getUser().getEmail();
            String subject = "Deposit Successful";
            String body = String.format(
                    "Dear %s,\n\nA deposit of %s has been credited to your account %s on %s.\n\nThank you for banking with us.",
                    account.getUser().getFirstName(),
                    amount.toPlainString(),
                    account.getAccountNumber(),
                    LocalDateTime.now()
            );
            emailService.sendEmail(userEmail, subject, body);
        }

        return updatedAccount;
    }

    @Transactional
    public Account withdraw(Long accountId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive.");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + accountId));

        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds for withdrawal. Current balance: " + account.getBalance());
        }

        account.setBalance(account.getBalance().subtract(amount));
        account.setUpdatedAt(LocalDateTime.now());
        Account updatedAccount = accountRepository.save(account);

        createAndSaveChainedTransaction(
                TransactionType.WITHDRAWAL,
                amount,
                description != null ? description : "Cash Withdrawal",
                updatedAccount,
                null
        );

        // Send withdrawal email notification
        if (account.getUser() != null && account.getUser().getEmail() != null) {
            String userEmail = account.getUser().getEmail();
            String subject = "Withdrawal Alert";
            String body = String.format(
                    "Dear %s,\n\nA withdrawal of %s has been debited from your account %s on %s.\n\nThank you for banking with us.",
                    account.getUser().getFirstName(),
                    amount.toPlainString(),
                    account.getAccountNumber(),
                    LocalDateTime.now()
            );
            emailService.sendEmail(userEmail, subject, body);
        }

        return updatedAccount;
    }

    @Transactional
    public void transfer(Long sourceAccountId, Long destinationAccountId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive.");
        }

        Account sourceAccount = accountRepository.findById(sourceAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Source Account not found with ID: " + sourceAccountId));
        Account destinationAccount = accountRepository.findById(destinationAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Destination Account not found with ID: " + destinationAccountId));

        if (sourceAccount.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds in source account " + sourceAccount.getAccountNumber() + " for transfer.");
        }

        sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
        destinationAccount.setBalance(destinationAccount.getBalance().add(amount));

        sourceAccount.setUpdatedAt(LocalDateTime.now());
        destinationAccount.setUpdatedAt(LocalDateTime.now());

        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);

        createAndSaveChainedTransaction(
                TransactionType.TRANSFER,
                amount,
                description != null ? description : "Funds Transfer",
                sourceAccount,
                destinationAccount
        );

        // Notify sender
        if (sourceAccount.getUser() != null && sourceAccount.getUser().getEmail() != null) {
            String senderEmail = sourceAccount.getUser().getEmail();
            String senderBody = String.format(
                    "Dear %s,\n\nYou have transferred %s to account %s on %s.\n\nThank you for banking with us.",
                    sourceAccount.getUser().getFirstName(),
                    amount.toPlainString(),
                    destinationAccount.getAccountNumber(),
                    LocalDateTime.now()
            );
            emailService.sendEmail(senderEmail, "Funds Transferred", senderBody);
        }

        // Notify receiver
        if (destinationAccount.getUser() != null && destinationAccount.getUser().getEmail() != null) {
            String receiverEmail = destinationAccount.getUser().getEmail();
            String receiverBody = String.format(
                    "Dear %s,\n\nAn amount of %s has been credited to your account %s from account %s on %s.\n\nThank you for banking with us.",
                    destinationAccount.getUser().getFirstName(),
                    amount.toPlainString(),
                    destinationAccount.getAccountNumber(),
                    sourceAccount.getAccountNumber(),
                    LocalDateTime.now()
            );
            emailService.sendEmail(receiverEmail, "Funds Received", receiverBody);
        }
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAllByOrderByTransactionDateAscIdAsc();
    }

    public List<Transaction> getTransactionsForAccount(Long accountId) {
        List<Transaction> allTransactions = transactionRepository.findAllByOrderByTransactionDateAscIdAsc();
        return allTransactions.stream()
                .filter(t -> (t.getSourceAccount() != null && t.getSourceAccount().getId().equals(accountId)) ||
                        (t.getDestinationAccount() != null && t.getDestinationAccount().getId().equals(accountId)))
                .sorted((t1, t2) -> t2.getTransactionDate().compareTo(t1.getTransactionDate()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsForUser(Long userId) {
        List<Account> userAccounts = accountRepository.findByUserId(userId);
        List<Transaction> allUserTransactions = new ArrayList<>();

        for (Account account : userAccounts) {
            List<Transaction> accountTransactions = getTransactionsForAccount(account.getId());
            allUserTransactions.addAll(accountTransactions);
        }

        return allUserTransactions.stream()
                .distinct()
                .sorted((t1, t2) -> t2.getTransactionDate().compareTo(t1.getTransactionDate()))
                .collect(Collectors.toList());
    }

    /**
     * Verifies the integrity of the transaction ledger.
     * This method re-calculates hashes and checks if the chain is valid.
     * It uses @EntityGraph in the repository to eager fetch associated data.
     * It also handles potential null previous hashes gracefully.
     * @return true if the ledger is intact, false if tampering is detected.
     */
    @Transactional(readOnly = true)
    /**
     * Verifies the integrity of the transaction ledger.
     * Use pagination to avoid OutOfMemoryError for large datasets.
     * @return true if the ledger is intact, false if tampering is detected.
     */
    @Transactional(readOnly = true)
    public boolean verifyLedgerIntegrity() {
        int pageSize = 1000;
        int pageNumber = 0;
        boolean hasMore = true;
        String expectedPreviousHash = "0";

        while (hasMore) {
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(pageNumber, pageSize, 
                org.springframework.data.domain.Sort.by("transactionDate").ascending().and(org.springframework.data.domain.Sort.by("id").ascending()));
            
            org.springframework.data.domain.Page<Transaction> page = transactionRepository.findAll(pageable);
            List<Transaction> transactions = page.getContent();

            if (transactions.isEmpty()) {
                hasMore = false;
                break;
            }

            for (Transaction currentTransaction : transactions) {
                String actualPreviousHash = currentTransaction.getPreviousTransactionHash();

                boolean previousHashMatches = (actualPreviousHash == null && expectedPreviousHash.equals("0")) ||
                        (actualPreviousHash != null && actualPreviousHash.equals(expectedPreviousHash));

                if (!previousHashMatches) {
                    System.err.println("TAMPERING DETECTED: Previous hash mismatch for transaction ID " + currentTransaction.getId());
                    System.err.println("  Expected previous hash: " + expectedPreviousHash);
                    System.err.println("  Actual previous hash in DB: " + (actualPreviousHash == null ? "NULL" : actualPreviousHash));
                    return false;
                }

                Long sourceAccountId = (currentTransaction.getSourceAccount() != null) ? currentTransaction.getSourceAccount().getId() : null;
                Long destinationAccountId = (currentTransaction.getDestinationAccount() != null) ? currentTransaction.getDestinationAccount().getId() : null;

                String recalculatedHashInput = HashUtil.generateTransactionDataString(
                        currentTransaction.getId(),
                        currentTransaction.getTransactionType(),
                        currentTransaction.getAmount(),
                        currentTransaction.getDescription(),
                        sourceAccountId,
                        destinationAccountId,
                        currentTransaction.getTransactionDate()
                );

                String expectedCurrentHash = HashUtil.calculateSHA256Hash(recalculatedHashInput);
                String actualCurrentHash = currentTransaction.getTransactionHash();

                if (actualCurrentHash == null || !actualCurrentHash.equals(expectedCurrentHash)) {
                    System.err.println("TAMPERING DETECTED: Current hash mismatch for transaction ID " + currentTransaction.getId());
                    return false;
                }

                expectedPreviousHash = currentTransaction.getTransactionHash();
            }

            if (page.hasNext()) {
                pageNumber++;
            } else {
                hasMore = false;
            }
        }
        return true;
    }

    // NEW: Transaction Reversal Logic (from your provided code - ensuring it calls createAndSaveChainedTransaction)
    @Transactional
    public void reverseTransaction(Long transactionId) {
        Transaction originalTransaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Original transaction not found with ID: " + transactionId));

        if (originalTransaction.isReversed()) {
            throw new IllegalStateException("Transaction " + transactionId + " has already been reversed.");
        }
        if (!originalTransaction.getStatus().equals("COMPLETED")) {
            throw new IllegalStateException("Transaction " + transactionId + " cannot be reversed as its status is not COMPLETED.");
        }
        if (originalTransaction.getOriginalTransactionId() != null) {
            throw new IllegalStateException("Transaction " + transactionId + " is itself a reversal and cannot be reversed again.");
        }

        BigDecimal amount = originalTransaction.getAmount();
        Account sourceAccount = originalTransaction.getSourceAccount();
        Account destinationAccount = originalTransaction.getDestinationAccount();
        TransactionType originalTxnType = originalTransaction.getTransactionType();

        String reversalDescription = "Reversal of Transaction ID " + originalTransaction.getId() + ": " + originalTransaction.getDescription();

        // Prepare reversal transaction details
        TransactionType reversalTxnType;
        Account reversalSourceAccount = null;
        Account reversalDestinationAccount = null;

        switch (originalTxnType) {
            case TRANSFER:
                if (sourceAccount == null || destinationAccount == null) {
                    throw new IllegalArgumentException("Transfer transaction must have both source and destination accounts.");
                }
                if (destinationAccount.getBalance().compareTo(amount) < 0) {
                    throw new IllegalStateException("Destination account (" + destinationAccount.getAccountNumber() + ") has insufficient funds (" + destinationAccount.getBalance() + ") for transfer reversal of amount " + amount + ".");
                }

                destinationAccount.setBalance(destinationAccount.getBalance().subtract(amount));
                sourceAccount.setBalance(sourceAccount.getBalance().add(amount));

                accountRepository.save(destinationAccount);
                accountRepository.save(sourceAccount);

                reversalTxnType = TransactionType.TRANSFER_REVERSAL;
                reversalSourceAccount = destinationAccount; // Source of reversal is original destination
                reversalDestinationAccount = sourceAccount; // Destination of reversal is original source
                break;

            case DEPOSIT:
                if (destinationAccount == null) {
                    throw new IllegalArgumentException("Deposit transaction must have a destination account.");
                }
                if (destinationAccount.getBalance().compareTo(amount) < 0) {
                    throw new IllegalStateException("Account (" + destinationAccount.getAccountNumber() + ") has insufficient funds (" + destinationAccount.getBalance() + ") for deposit reversal of amount " + amount + ".");
                }

                destinationAccount.setBalance(destinationAccount.getBalance().subtract(amount));
                accountRepository.save(destinationAccount);

                reversalTxnType = TransactionType.DEPOSIT_REVERSAL;
                reversalSourceAccount = destinationAccount; // The account is the source of the reversal outflow
                reversalDestinationAccount = null; // No destination for a deposit reversal
                break;

            case WITHDRAWAL:
                if (sourceAccount == null) {
                    throw new IllegalArgumentException("Withdrawal transaction must have a source account.");
                }

                sourceAccount.setBalance(sourceAccount.getBalance().add(amount));
                accountRepository.save(sourceAccount);

                reversalTxnType = TransactionType.WITHDRAWAL_REVERSAL;
                reversalSourceAccount = null; // No source for a withdrawal reversal
                reversalDestinationAccount = sourceAccount; // The account is the destination of the reversal inflow
                break;

            default:
                throw new IllegalArgumentException("Reversal logic not implemented for transaction type: " + originalTxnType.name());
        }

        // Mark the original transaction as reversed and update its status
        originalTransaction.setReversed(true);
        originalTransaction.setStatus("REVERSED");
        transactionRepository.save(originalTransaction);

        // Create the new reversal transaction
        Transaction reversalTxn = createAndSaveChainedTransaction(
                reversalTxnType,
                amount,
                reversalDescription,
                reversalSourceAccount,
                reversalDestinationAccount
        );
        // Link the reversal transaction to its original
        reversalTxn.setOriginalTransactionId(originalTransaction.getId());
        transactionRepository.save(reversalTxn); // Save again to update originalTransactionId
    }

    // NEW: Method to get transactions for a specific account within a date range
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsForAccountInDateRange(Long accountId, Instant startDate, Instant endDate) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + accountId));

        // Use the new repository method from TransactionRepository
        return transactionRepository.findBySourceAccountAndTransactionDateBetweenOrDestinationAccountAndTransactionDateBetween(
                account, startDate, endDate,
                account, startDate, endDate
        );
    }

    // NEW: Method to calculate opening and closing balances and ledger hash for an account statement
    // Returns a Map<String, Object> to accommodate both BigDecimal balances and String ledger hash
    @Transactional(readOnly = true)
    public Map<String, Object> calculateStatementBalances(Long accountId, Instant startDate, Instant endDate) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + accountId));

        // Current balance of the account is the closing balance for the statement period
        BigDecimal closingBalance = account.getBalance();

        // Get transactions within the specified period
        List<Transaction> transactionsInPeriod = getTransactionsForAccountInDateRange(accountId, startDate, endDate);

        // Calculate the net change in balance during the period from the perspective of this account
        BigDecimal netChangeInPeriod = BigDecimal.ZERO;
        for (Transaction txn : transactionsInPeriod) {
            if (txn.getSourceAccount() != null && txn.getSourceAccount().getId().equals(accountId)) {
                // If this account is the source, amount left the account
                netChangeInPeriod = netChangeInPeriod.subtract(txn.getAmount());
            } else if (txn.getDestinationAccount() != null && txn.getDestinationAccount().getId().equals(accountId)) {
                // If this account is the destination, amount entered the account
                netChangeInPeriod = netChangeInPeriod.add(txn.getAmount());
            }
        }

        // Opening balance = Closing Balance - Net Change in Period
        BigDecimal openingBalance = closingBalance.subtract(netChangeInPeriod);

        // Calculate the ledger hash for transactions within the statement period
        StringBuilder ledgerHashInputBuilder = new StringBuilder();
        transactionsInPeriod.stream()
                .sorted((t1, t2) -> { // Ensure consistent order for hashing
                    int dateComparison = t1.getTransactionDate().compareTo(t2.getTransactionDate());
                    if (dateComparison != 0) return dateComparison;
                    return t1.getId().compareTo(t2.getId()); // Fallback to ID for stable sort
                })
                .forEach(txn -> {
                    // Use the same logic as verifyLedgerIntegrity for consistency
                    Long sourceAccountId = (txn.getSourceAccount() != null) ? txn.getSourceAccount().getId() : null;
                    Long destinationAccountId = (txn.getDestinationAccount() != null) ? txn.getDestinationAccount().getId() : null;
                    ledgerHashInputBuilder.append(HashUtil.generateTransactionDataString(
                            txn.getId(), txn.getTransactionType(), txn.getAmount(),
                            txn.getDescription(), sourceAccountId, destinationAccountId,
                            txn.getTransactionDate()
                    ));
                });

        String ledgerHash = HashUtil.calculateSHA256Hash(ledgerHashInputBuilder.toString());

        Map<String, Object> statementSummary = new HashMap<>();
        statementSummary.put("openingBalance", openingBalance);
        statementSummary.put("closingBalance", closingBalance);
        statementSummary.put("ledgerHash", ledgerHash); // Store as String

        return statementSummary;
    }
}