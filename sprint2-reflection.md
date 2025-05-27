## feat: Implement core transaction management functionalities

This commit introduces the full suite of financial transaction capabilities to the banking system, allowing users to deposit, withdraw, and transfer funds, along with viewing transaction history.

Key changes include:

- **New Domain Models:**
    - `Transaction.java`: Entity to record detailed information for each financial transaction (amount, type, date, source/destination accounts).
    - `TransactionType.java`: Enum to categorize transactions (DEPOSIT, WITHDRAWAL, TRANSFER).

- **Data Access Layer:**
    - `TransactionRepository.java`: JPA repository for `Transaction` entity, including a custom query to retrieve all transactions related to a specific account.

- **Business Logic Layer:**
    - `TransactionService.java`: New service handling the core business logic for all transaction types:
        - `deposit()`: Adds funds to an account and records the transaction.
        - `withdraw()`: Deducts funds from an account (with insufficient funds check) and records the transaction.
        - `transfer()`: Atomically moves funds between two accounts (with balance checks) and records the transfer.
        - `getTransactionsForAccount()`: Retrieves historical transactions for a given account.
    - Utilizes `@Transactional` annotation to ensure atomicity and data integrity for financial operations.
    - Implements robust error handling using `IllegalArgumentException`.

- **Web Layer (Controller & UI):**
    - `AccountController.java`:
        - Injected `TransactionService`.
        - Added new GET/POST endpoints for `/accounts/{id}/deposit`, `/accounts/{id}/withdraw`, `/accounts/{id}/transfer`, and `/accounts/{id}/transactions`.
        - Implemented redirecting and flash attributes (`success`, `error`) for user feedback after transactions.
        - Refactored constructors for proper Spring dependency injection (single `@Autowired` constructor).
    - **New Thymeleaf Templates:**
        - `deposit.html`: Form for depositing funds.
        - `withdraw.html`: Form for withdrawing funds.
        - `transfer.html`: Form for transferring funds between accounts.
        - `account-transactions.html`: Displays a detailed list of transactions for a selected account.
    - `account-details.html`: Updated to include direct action buttons for Deposit, Withdrawal, Transfer, and View Transactions, enhancing user navigation.

This completes the core transaction management features for the banking system.
