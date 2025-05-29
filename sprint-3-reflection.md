# Sprint 4: User Authentication & Authorization - Documentation

This document reflects on the implementation of user authentication and basic role-based authorization for the banking system, detailing key achievements and challenges encountered during this development phase.

## Overview

This sprint focused on establishing a secure foundation for the application by integrating Spring Security. The primary goal was to enable user login/logout, ensure data isolation (users only see their own accounts), and lay the groundwork for role-based access control.

## Key Achievements

* **Secure User Authentication:** Implemented robust user login and logout functionality using Spring Security.
* **Account Ownership Filtering:** Successfully configured the system to display only the bank accounts associated with the currently logged-in user, preventing unauthorized access to other users' financial data.
* **Self-Service Account Creation:** Enabled authenticated users to create new bank accounts that are automatically linked to their profile, removing the need to select an owner from a dropdown.
* **Basic Role-Based Access Control (RBAC):** Established initial URL-based authorization rules in `SecurityConfig.java` to restrict access to certain paths (e.g., `/admin/**` for `ROLE_ADMIN` only), demonstrating the system's ability to differentiate user permissions.

## Challenges Faced & Solutions Implemented

This section outlines specific technical challenges encountered and the solutions applied to overcome them.

### 1. Login Redirect Loop (`ERR_TOO_MANY_REDIRECTS`)

* **Problem:** After configuring Spring Security, navigating to `/login` resulted in an infinite redirect loop. This occurred because Spring Security was redirecting to `/login` (as configured) but there was no dedicated controller or Thymeleaf template to serve the actual login page.
* **Solution:** Created a `LoginController` with `@GetMapping("/login")` and `@GetMapping("/register")` methods to explicitly render `login.html` and `register.html` Thymeleaf templates.

### 2. Database Schema Mismatches (Empty Passwords/Emails, Unknown Columns)

* **Problem:** New user registrations resulted in empty `password` and `email` fields in the MySQL database. Subsequent login attempts failed due to an "Unknown column 'u1_0.updated_at'" error and later "Field `password_hash` doesn't have a default value."
* **Solution:**
    * **Empty Fields:** The `POST /register` endpoint in `LoginController` was not correctly binding form data to the `User` object. This was fixed by ensuring `register.html` used `th:object="${user}"` and `th:field="*{fieldName}"` for input binding, and the controller method correctly used `@ModelAttribute("user") User user`.
    * **Unknown Column `updated_at`:** The `users` table in MySQL was missing the `updated_at` column (and `created_at` was sometimes inconsistent) despite `ddl-auto=update`. This was manually corrected by running `ALTER TABLE users ADD COLUMN updated_at DATETIME;` and `ALTER TABLE users ADD COLUMN created_at DATETIME;` in MySQL.
    * **`password_hash` Error:** An old, conflicting `password_hash` column with a `NOT NULL` constraint existed in the `users` table, preventing inserts. This was resolved by explicitly dropping the column: `ALTER TABLE users DROP COLUMN password_hash;`.
    * **`updated_at` NULL Value Error:** Existing `NULL` values in `updated_at` prevented setting the column to `NOT NULL`. This was fixed by updating existing `NULL` values to `NOW()` before altering the column: `UPDATE users SET updated_at = NOW() WHERE updated_at IS NULL;` followed by `ALTER TABLE users MODIFY COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;`.

### 3. Ambiguous Mapping for `POST /accounts/create`

* **Problem:** A `BeanCreationException: Ambiguous mapping` error occurred during application startup, indicating two `createAccount` methods in `AccountController` were mapped to `POST /accounts/create`.
* **Solution:** Identified and removed the duplicate `createAccount` method, leaving only the one that retrieves the user ID from the Spring Security context.

### 4. `Cannot find @interface method 'defaultValue()'` in `User.java`

* **Problem:** Compilation failed with this error, despite `jakarta.persistence-api-3.1.0.jar` being present and correct in the local Maven repository. This indicated a deep-seated classpath or compiler interpretation issue.
* **Solution:** As a workaround, the `defaultValue = "ROLE_CUSTOMER"` attribute was removed from the `@Column` annotation for the `role` field in `User.java`. The default role assignment is now handled programmatically in the `User` constructor and the `@PrePersist` lifecycle callback, achieving the same functional outcome.

### 5. Thymeleaf `sec:authorize` Not Processing

* **Problem:** Links intended only for `ADMIN` or `EMPLOYEE` roles were still visible to `ROLE_CUSTOMER` users on the `dashboard.html` page, even though backend security correctly blocked access (resulting in a 403).
* **Solution:** The `thymeleaf-extras-springsecurity6` dependency was missing from `pom.xml`. Adding this dependency allowed Thymeleaf to correctly process the `sec:authorize` attributes and conditionally render content based on the authenticated user's roles. A `mvn clean install -U` was necessary to ensure the dependency was properly downloaded and recognized.

## Next Steps

With authentication and basic authorization in place, the next steps will focus on refining access control and building out role-specific functionalities:

1.  **Granular Account Access Control:** Implement ownership checks for all individual account operations (e.g., `details`, `edit`, `deposit`, `withdraw`, `transfer`) to ensure a user can only interact with accounts they own. This will involve modifying `AccountService` methods to accept the authenticated `User` object and perform validation.
2.  **Admin Panel Development:**
    * Create dedicated `AdminController` methods and Thymeleaf templates for managing users (e.g., listing all users, editing user details including roles, deleting users).
    * Apply `@PreAuthorize("hasRole('ADMIN')")` to these methods to enforce administrative access.
3.  **Employee Panel Development:**
    * Create `EmployeeController` methods and templates for employee-specific tasks (e.g., viewing all customer accounts for support, approving transactions).
    * Apply `@PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")` to these methods.
4.  **UI Enhancements:** Further refine the navigation and visibility of links/buttons in Thymeleaf templates based on user roles using `sec:authorize`.
