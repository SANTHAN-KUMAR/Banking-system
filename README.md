# Hybrid Banking System: Project Roadmap
[![Maven Package](https://github.com/SANTHAN-KUMAR/Banking-system/actions/workflows/maven-publish.yml/badge.svg)](https://github.com/SANTHAN-KUMAR/Banking-system/actions/workflows/maven-publish.yml)


# This project is **not** a normal banking system. Here’s what sets it apart and exactly what it’s doing:

---

## **What Is This Project Doing?**

It’s building a **Hybrid Banking System** that combines the best features of both traditional banking and blockchain technology. This means:

- **All core banking operations** (user management, account creation, deposits, withdrawals, transfers, transaction history, KYC, etc.) are present, just like a normal banking system.
- **PLUS:** Every transaction is cryptographically chained (blockchain-style), making the ledger immutable and tamper-evident. This is NOT just a log—it’s a full audit trail where every transaction hash depends on the previous, so any change breaks the chain.
- **PLUS:** Real-time fraud detection is built-in, with automated alerts for suspicious activity (large transfers, frequent transactions, transfers to new accounts, etc.).
- **PLUS:** Controlled reversibility—unlike pure blockchain, authorized admins/employees can reverse transactions (with audit trail), reflecting real banking needs (disputes, errors, fraud).

---

## **How Is This Different From a Standard Banking System?**

### **Standard Banking System**
- **Audit trails** can be edited by insiders (DB admins, devs).
- Transactions can be changed or deleted—hard to spot tampering.
- Fraud detection is often manual or basic.
- Reversibility is possible, but not always clearly tracked.
- Security is heavily reliant on access controls and compliance audits.

### **This Hybrid System**
- **Immutability:** Every transaction is part of a hash chain (like a blockchain), so even if someone has DB access, tampering is instantly detectable.
- **Auditability:** Anyone can verify the ledger’s integrity at any time via the admin interface. Ledger verification recalculates hashes and detects changes.
- **Real-time Automated Fraud Alerts:** System detects and flags suspicious transactions automatically, not just after-the-fact.
- **Controlled Reversibility:** Unlike blockchain (no reversals possible), authorized reversals are possible—but every reversal is recorded as a new, chained transaction, with a clear link to the original.
- **Hybrid Compliance:** Fuses blockchain’s transparency with the compliance and customer-protection features of banking (KYC, user roles, admin controls).

---

## **What’s the Unique Value Proposition?**

**1. Tamper-Evident Ledger**
   - All transactions are chained with cryptographic hashes.
   - No insider (not even a DBA) can alter history without detection.

**2. Real-Time Fraud Monitoring**
   - Automated alert engine flags risky behavior instantly (large/frequent transactions, new account activity).

**3. Hybrid Reversibility**
   - Admin/employee can reverse transactions (like real banks), but every reversal is part of the immutable ledger.

**4. Strong Identity & Authorization**
   - KYC, RBAC, PINs, and (planned) 2FA/OTP.

**5. Blockchain Benefits, Banking Practicality**
   - You get blockchain-style proof and security, but with regulatory flexibility and customer protections only banks can offer.

**6. Foundation for Advanced Features**
   - Conceptual AI-based fraud detection, asset tokenization, open banking APIs, and more (see [Final Scope.md](Final%20Scope.md)).

---

### **In summary:**
> **This is a next-gen, tamper-proof banking system that brings together the strengths of both blockchain and traditional banking. It’s not just “digital banking”—it’s auditable, secure, and future-ready.**

If you want a normal banking system, you can use only the CRUD/account/transaction features. If you want to **prove to auditors, customers, or regulators that your records are 100% untampered and modernize for the future**, this hybrid approach is unique.

## Recent update : Latest update with enhanced features (v3.4) - refer description for full udpate log.

### (The recent update consists Secret keys in order to pass the test cases, so they were removed and the build is failing. 


Comparison to Existing Projects
Project	Similarity %	Key Differences
Stanford ERC-20R/721R	40%	Academic vs production, limited scope vs full banking
JPMorgan Onyx	35%	Institutional payments vs retail banking, no reversibility
XinFin Network	30%	Trade finance focus vs comprehensive banking
Italian Spunta	25%	Interbank reconciliation only vs full banking operations
R3 Corda	30%	Not technically blockchain, purely private

The approach is unique enough to be valuable, but manage expectations:

    Individual features: 20-40% are novel implementations of known concepts

    System integration: 70-80% unique in how everything works together

    Market gap: Genuine need that existing solutions don't fully address

    Technical innovation: Moderate to high, not revolutionary

    Business value: High - solves real banking industry problems

The uniqueness score is of 71.9% reflects a genuinely innovative solution that combines existing technologies in a novel way to solve specific problems. This is actually more valuable than purely novel research because it addresses real-world banking needs with practical implementations.


The controlled reversibility + immutable audit trail combination is your strongest differentiator - it's the kind of innovation that could genuinely transform banking infrastructure.
---

## Sample architecture of the project

![Editor _ Mermaid Chart-2025-06-27-173825](https://github.com/user-attachments/assets/ba299f4b-be23-478b-85eb-59ba17c22062)



## Currently - done with implementations listed in temp_goals.md

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

## ~~Phase 3: User Authentication & Authorization (Security)~~ (Completed - enhancements are needed)

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
