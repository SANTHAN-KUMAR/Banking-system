

### Sprint 1: Account Management Update (Quick Reference)

**Overall Goal:** To build the core functionality for managing bank accounts, including creating, viewing, and updating basic account details.

---

#### 1. New Core Component: The `Account` Entity

* **What it is:** This is the blueprint for a bank account in our system. It lives in `src/main/java/com/santhan/banking_system/model/Account.java`.
* **Key Details:**
    * `id`: Unique number for each account (database ID).
    * `accountNumber`: A unique number for customers to identify their account (e.g., "1234567890"). We generate this automatically.
    * `accountType`: What kind of account it is (e.g., Savings, Checking).
    * `balance`: How much money is in the account. **Important: This is a `BigDecimal` for accuracy, not `double` or `float`!**
    * `user`: Who owns this account.
    * `createdAt`, `updatedAt`: Timestamps to track when the account was created and last changed.

#### 2. Account Type: The `AccountType` Enum

* **What it is:** A simple list of predefined account types (`SAVINGS`, `CHECKING`, etc.). It lives in `src/main/java/com/santhan/banking_system/model/AccountType.java`.
* **Why we use it:** Prevents typos, ensures consistency, and makes it easy to add new types later without changing lots of code.

#### 3. Connecting Users to Accounts: The Relationship

* **What we did:** We linked the `User` entity to the `Account` entity.
* **How:**
    * In `User.java`: Added `@OneToMany` to say "one user can have many accounts."
    * In `Account.java`: Added `@ManyToOne` and `@JoinColumn` to say "many accounts belong to one user."
* **Why it's important:** This is fundamental for a banking system – knowing who owns which account.

---

#### 4. Talking to the Database: The `AccountRepository`

* **What it is:** Our direct connection to the database for `Account` data. It lives in `src/main/java/com/santhan/banking_system/repository/AccountRepository.java`.
* **Key Additions:**
    * `findByUser(User user)`: To find all accounts belonging to a specific user.
    * `findByAccountNumber(String accountNumber)`: To find an account using its unique account number.
* **Why it's important:** Spring Data JPA automatically writes the complex database queries for us based on these method names.

#### 5. The "Brain" of Account Operations: The `AccountService`

* **What it is:** Handles all the business logic for accounts. It lives in `src/main/java/com/santhan/banking_system/service/AccountService.java`.
* **Key Methods Added/Updated:**
    * `createAccount()`: Creates a new account, generates a unique account number, links it to a user, and ensures the balance is non-negative.
    * `getAllAccounts()`: Fetches all accounts from the database.
    * `getAccountById()`: Fetches a single account by its database ID.
    * `getAccountsByUserId()`: Fetches all accounts for a specific user.
    * **`updateAccountDetails()`:** **Crucial!** This method handles updating an account.
        * **Important Protection:** It specifically **DOES NOT** allow direct modification of the `balance`. Balance changes must happen through dedicated transaction methods (deposit, withdrawal) later. This protects financial integrity.
    * `deleteAccount()`: Removes an account (though we haven't added a button for this yet).
* **Why it's important:** This layer keeps our business rules separate from web requests or database details.

---

#### 6. Handling Web Requests: The `AccountController`

* **What it is:** The "traffic cop" that receives requests from the web browser and sends back HTML pages. It lives in `src/main/java/com/santhan/banking_system/controller/AccountController.java`.
* **Key Methods Added/Updated:**
    * `@GetMapping("/accounts")`: Shows the list of all accounts.
    * `@GetMapping("/accounts/create")`: Displays the "Create New Account" form.
    * `@PostMapping("/accounts/create")`: Processes the submission of the "Create Account" form, handles validation (e.g., negative balance).
    * **`@GetMapping("/accounts/details/{id}")`**: Shows the detailed view of a single account.
    * **`@GetMapping("/accounts/edit/{id}")`**: Displays the "Edit Account" form, pre-filling it with existing data.
    * **`@PostMapping("/accounts/update/{id}")`**: Processes the submission of the "Edit Account" form, calls the `AccountService` to save changes, and redirects back to the details page.
* **Why it's important:** Connects user actions in the browser to our backend logic.

---

#### 7. What the User Sees: Thymeleaf Templates

* **What they are:** HTML files that Spring fills with data from our Java code to create dynamic web pages. They live in `src/main/resources/templates/`.
* **Key Files Created/Updated:**
    * **`account-list.html`**: Displays the table of all accounts, now with "Details" and "Edit" buttons.
    * **`account-create.html`**: The form for creating a new account (this was the one we were missing!). It includes dropdowns for `AccountType` and `User` owner.
    * **`account-details.html`**: Shows a clean, detailed view of a single account.
    * **`account-edit.html`**: The form for updating account details.
        * **Important Protection:** The "Account Number" and "Balance" fields are set as `readonly` in this form to prevent accidental or malicious changes.

---

**In a Nutshell:**

We added `Account` as a new central piece of data, connected it to `User`s, built the database tools (`Repository`), the business rules (`Service`), the web routes (`Controller`), and the user-facing pages (`Templates`) for creating, viewing, and safely updating account information.
