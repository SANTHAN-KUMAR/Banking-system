# Real-Time Banking System: Project Roadmap

This document outlines the development roadmap for our real-time banking system, focusing on essential banking operations. We'll be taking an iterative approach, building out one major module at a time.

---
## Currently in Phase - 3
## ~~Phase 0: Foundation~~ (Completed)

This phase covers the core components we've already established or are in the process of building.

### User Management (Basic)
* **Description:** Ability to create, view, and store basic user (customer) profiles.
* **Status:** Partially implemented (CRUD for users, no login/security yet).
* **Key Components:** `User` Entity, `UserRepository`, `UserService`, `WebController` (for user forms/lists).

### Database Integration
* **Description:** Connecting the Spring Boot application to a MySQL database for persistent storage.
* **Status:** Implemented and functional.
* **Key Components:** `application.properties` (datasource config), JPA, Hibernate.

### Web Interface (Thymeleaf/MVC)
* **Description:** Basic HTML pages rendered dynamically using Thymeleaf for user interaction.
* **Status:** Implemented (User list/creation pages).
* **Key Components:** Spring MVC (`@Controller`, `@GetMapping`, `@PostMapping`), Thymeleaf templates.

---

## ~~Phase 1: Core Account Management~~ (Completed)

This phase expands our system to manage bank accounts.

### 1.1 Account Creation & Listing
* **Description:** Allow users to create different types of bank accounts (e.g., Savings, Checking) and view a list of all existing accounts.
* **Data Model:** `Account` entity (ID, account number, type, balance, owner `User`).
* **Business Logic:** Generate unique account numbers, link accounts to specific users, initialize balances.
* **User Interface:** Form to create an account, table to list all accounts.
* **Status:** Currently implementing.

### 1.2 Account Details & Updates
* **Description:** View detailed information for a single account; ability to update non-financial account details (e.g., account type if allowed, but **NOT** balance directly).
* **Data Model:** `Account` entity.
* **Business Logic:** Retrieve account by ID.
* **User Interface:** Dedicated "Account Details" page, form for editing.

---

## ~~Phase 2: Transaction Processing~~ (Completed)

This phase is the core of any banking system, focusing on handling financial transactions. It requires careful handling of financial logic and atomicity.

### 2.1 Deposits
* **Description:** Add funds to an existing account.
* **Data Model:** `Transaction` entity (ID, amount, type, timestamp, associated account). `Account` (balance update).
* **Business Logic:**
    * Validate input (positive amount, valid account).
    * Increment `Account` balance.
    * Create `Transaction` record for the deposit.
    * **Crucial:** Ensure atomicity (`@Transactional`) for balance update and transaction record creation.
* **User Interface:** Form for deposit (account selection, amount input).

### 2.2 Withdrawals
* **Description:** Remove funds from an existing account.
* **Data Model:** `Transaction` entity, `Account` (balance update).
* **Business Logic:**
    * Validate input (positive amount, valid account).
    * **Crucial:** Check for sufficient funds (`balance >= amount`).
    * Decrement `Account` balance.
    * Create `Transaction` record for the withdrawal.
    * **Crucial:** Ensure atomicity (`@Transactional`).
* **User Interface:** Form for withdrawal (account selection, amount input).

### 2.3 Transfers (Internal)
* **Description:** Move funds between two accounts within the same bank (our system).
* **Data Model:** `Transaction` entity (two records for one transfer), `Account` (two balance updates).
* **Business Logic:**
    * Validate input (positive amount, valid source/destination accounts).
    * **Crucial:** Perform withdrawal from source, deposit to destination.
    * Create two `Transaction` records (one debit, one credit).
    * **Crucial:** Ensure atomicity (`@Transactional`) for both balance updates and both transaction records. If one part fails, everything rolls back.
* **User Interface:** Form for transfer (source account, destination account, amount).

### 2.4 Transaction History
* **Description:** View all transactions (deposits, withdrawals, transfers) associated with a specific account or user.
* **Data Model:** `Transaction` entity.
* **Business Logic:** Retrieve transactions by account/user, potentially with filtering/sorting.
* **User Interface:** Table displaying transaction details.

---

## Phase 3: User Authentication & Authorization (Security)

This phase is paramount for a real banking system, ensuring only authorized users can access their data and perform allowed operations.

### 3.1 User Registration & Login
* **Description:** Allow new users to register securely, and existing users to log in.
* **Data Model:** `User` (with password hashing).
* **Business Logic:** Password hashing (e.g., BCrypt), session management.
* **Key Components:** Spring Security integration, Custom `UserDetailsService`, `WebSecurityConfigurerAdapter` (or newer `SecurityFilterChain`).
* **User Interface:** Registration form, Login page.

### 3.2 Role-Based Access Control (RBAC)
* **Description:** Define different roles (e.g., `ROLE_CUSTOMER`, `ROLE_EMPLOYEE`, `ROLE_ADMIN`) and restrict access to certain functionalities/pages based on the logged-in user's role.
* **Data Model:** `Role` entity, Many-to-many relationship between `User` and `Role`.
* **Business Logic:** Assign roles to users, secure methods/URLs using Spring Security annotations (`@PreAuthorize`).
* **User Interface:** (Potentially) Admin pages for role assignment.

### 3.3 Secure Session Management
* **Description:** Securely manage user sessions after login.
* **Business Logic:** Spring Security handles this largely automatically (CSRF protection, session fixation protection).

---

## Phase 4: Advanced Features & Refinements

Once the core functionalities are stable, we'll focus on adding more real-world elements and improving the system.

### 4.1 Account Statements
* **Description:** Generate a summary of transactions for a specific period for an account.
* **Business Logic:** Query transactions within a date range, calculate opening/closing balances.
* **User Interface:** Filters for date range, display of statement.

### 4.2 User Profile Management
* **Description:** Allow users to view and update their profile details (e.g., email, address, but not username).
* **User Interface:** Profile viewing/editing forms.

### 4.3 Loan Management (Conceptual)
* **Description:** Basic loan application, approval, and repayment tracking.
* **Data Model:** `Loan` entity (amount, status, interest rate, repayment schedule), `LoanApplication` entity.
* **Business Logic:** Loan approval process, balance updates for loan disbursements/repayments.

### 4.4 Notifications (Conceptual)
* **Description:** Send email/SMS notifications for transactions, account changes.
* **Business Logic:** Integration with external messaging services.

### 4.5 Admin Panel
* **Description:** Interface for bank employees/admins to manage users, accounts, view system-wide transactions, and potentially approve/reject operations.
* **User Interface:** Separate admin dashboard.

---

## Phase 5: Error Handling, Logging, and Testing

These are ongoing and crucial activities that will be integrated throughout the development lifecycle.

### 5.1 Comprehensive Error Handling
* **Description:** Gracefully handle exceptions (e.g., "Insufficient Funds," "Account Not Found") and provide user-friendly error messages.
* **Implementation:** Custom exceptions, `@ControllerAdvice`.

### 5.2 Robust Logging
* **Description:** Log significant application events (transactions, security events, errors) for debugging and auditing.
* **Implementation:** Using SLF4J/Logback effectively.

### 5.3 Unit and Integration Testing
* **Description:** Write automated tests to ensure each component and the system as a whole works as expected.
* **Implementation:** JUnit, Mockito, Spring Boot Test.
