# Hybrid Banking System - Tamper-Proof, Fraud-Resistant Banking Platform

[![Build Status](https://github.com/SANTHAN-KUMAR/Banking-system/actions/workflows/maven-publish.yml/badge.svg)](https://github.com/SANTHAN-KUMAR/Banking-system/actions/workflows/maven-publish.yml)

---

## What Makes This Project Unique?

This is NOT a typical banking CRUD app.  
**It’s a hybrid system fusing blockchain-style immutability with real-world banking compliance and security.**

- **Immutable Ledger:** Every transaction is cryptographically chained; tampering is instantly detectable via hash-chain verification.
- **Real-Time Fraud Alerts:** Automated, rules-based fraud detection triggers alerts for large, frequent, or suspicious transactions.
- **Controlled Reversibility:** Only authorized admins/employees can reverse transactions—each reversal is immutably recorded and linked.
- **Role-Based Access (RBAC) & KYC:** Fine-grained permissions and full KYC workflow.
- **Proven Security:** Passwords & transaction PINs are hashed (BCrypt). No secrets in code.

---

## Features At-a-Glance

| Feature                        | Supported | Description                                          |
|--------------------------------|:---------:|------------------------------------------------------|
| User Registration/Login        |    ✅     | Role-based login, email & mobile verification        |
| Account Management             |    ✅     | Create, edit, view, and delete accounts              |
| Immutable Transaction Ledger   |    ✅     | Hash-chained, tamper-evident ledger                  |
| Real-Time Fraud Alerts         |    ✅     | Automated alerting for suspicious activity           |
| Controlled Transaction Reversal|    ✅     | Admin/employee can reverse, with audit trail         |
| KYC Compliance                 |    ✅     | KYC workflow and status tracking                     |
| REST API                       |    🚧     | Partial                                              |
| Admin/Employee Dashboard       |    ✅     | Full management of users/accounts/alerts             |

---

## 🏦 Architecture Overview

![Architecture Diagram](https://github.com/user-attachments/assets/ba299f4b-be23-478b-85eb-59ba17c22062)

- **Tech Stack:** Java 17+, Spring Boot, Spring Security, Spring Data JPA (Hibernate), Thymeleaf, MySQL/H2, Maven.
- **Core Services:** `UserService`, `AccountService`, `TransactionService`, `FraudAlertService`, `HashUtil`
- **Security:** BCrypt password & PIN hashing, RBAC via Spring Security, CSRF/session protection for web forms.

---

## 🔒 Security & Integrity Highlights

- **Password & Transaction PIN Hashing:** All credentials are stored securely (BCrypt, never plaintext).
- **Tamper-Evident Ledger:** Transaction hashes (SHA-256) chain every action; admin can verify ledger integrity from UI.
- **RBAC:** Only authorized users can access or modify sensitive data and actions.
- **No Hardcoded Secrets:** Remove or use environment variables for all credentials—see `application.properties` for examples.
- **CSRF & Session Security:** Enabled for all web forms; APIs secured via roles.

---

## Why This Matters (The “Why,” Not Just the “What”)

- **Immutable Audit Trail:** Proves integrity of every record—auditors, regulators, and customers can trust the data.
- **Proactive Security:** Fraud alerts are generated automatically, reducing risk and response times.
- **Reversibility with Accountability:** Transaction reversals are possible for compliance—but every reversal is visible and auditable.
- **Hybrid Compliance:** Combines the best of blockchain (transparency, immutability) and banking (control, reversibility, KYC).

---


---

## Quickstart: Running Locally

1. **Clone the repo:**
   ```bash
   git clone https://github.com/SANTHAN-KUMAR/Banking-system.git
   cd Banking-system/Project\ files
   ```

2. **Set up MySQL:**
   - Create a database: `banking_system`
   - Update `src/main/resources/application.properties` with your DB credentials.

3. **Configure mail (optional, for OTP):**
   - Set environment variables or edit `application.properties` for SMTP details.

4. **Run the app:**
   ```bash
   ./mvnw spring-boot:run
   ```
   or
   ```bash
   mvn spring-boot:run
   ```

5. **Access in browser:**  
   `http://localhost:8080`

---

## 👤 Demo/Test Credentials

> Use these for quick testing (change/remove in production):

| Role      | Username    | Password    | PIN    |
|-----------|-------------|-------------|--------|
| Admin     | `admin`     | `adminpass` | 123456 |
| Employee  | `employee`  | `emppass`   | 123456 |
| Customer  | `customer`  | `custpass`  | 123456 |

*If not present, create users via registration and promote via DB or admin dashboard.*

---

## 🧪 Running Tests

- **Basic Context Test:**  
  ```bash
  mvn test
  ```
  *(Yet to add more unit/integration tests in `/src/test/java` for services like `UserService`, `TransactionService`, `FraudAlertService`.)*

---

## 🏷️ Project Structure

- `src/main/java/com/santhan/banking_system/model` — Entities (User, Account, Transaction, FraudAlert, etc.)
- `src/main/java/com/santhan/banking_system/service` — Core business logic
- `src/main/java/com/santhan/banking_system/controller` — Web/API endpoints
- `src/main/resources/templates` — Thymeleaf HTML pages
- `src/main/resources/application.properties` — Config (DB, mail, etc.)

---

## 📝 Advanced/Future Features

- **PDF Statement Export with Ledger Hash:** (In progress/planned)
- **AI/ML-based Fraud Detection:** (See [`Final Scope.md`](Final%20Scope.md) for ideas)
- **Open Banking APIs, Asset Tokenization:** (Conceptualized for future—see [Final Scope.md](Final%20Scope.md))

---


---

## ⚠️ Security/Deployment Checklist

- [ ] Remove all test/demo credentials and secrets before production.
- [ ] Use environment variables for sensitive config (DB, SMTP, etc.).
- [ ] Run with `spring.jpa.hibernate.ddl-auto=validate` in production.
- [ ] Add HTTPS, advanced audit logging, and more tests for real deployment.

---

## 📚 References & Inspiration

| Project/Tech          | Similarity | Key Differences                           |
|-----------------------|:----------:|-------------------------------------------|
| Stanford ERC-20R/721R |    40%     | Academic, limited scope                   |
| JPMorgan Onyx         |    35%     | Institutional, no reversibility           |
| XinFin Network        |    30%     | Trade finance focus only                  |
| Italian Spunta        |    25%     | Only interbank reconciliation             |
| R3 Corda              |    30%     | Private DLT, no hybrid compliance         |


---

## 📄 Learn More

- [Final Scope.md](Final%20Scope.md) — Advanced/AI features, tokenization, open banking plans

---

# 🚀 How to Set Up and Run the Hybrid Banking System (For Absolute Beginners)

Welcome! If you have **zero Java or Spring Boot experience**, you can still set up and run this project by following these steps carefully.

---

## 1. **Prerequisites — Get These Tools First**

- **Java 17 or higher**  
  - Download: [Adoptium (Recommended)](https://adoptium.net/temurin/releases/?version=17)
  - After install:  
    - Open a terminal/cmd and run:  
      `java -version`  
      (Should print something like `17.0.x` or higher)

- **Maven (build tool)**  
  - *You don’t need to install it separately!* This project includes the Maven Wrapper (`mvnw` or `mvnw.cmd`), so you’re good as long as you have Java.

- **MySQL Server**  
  - Download: [MySQL Community Server](https://dev.mysql.com/downloads/mysql/)
  - After install:  
    - Use MySQL Workbench or CLI to create a database:  
      ```sql
      CREATE DATABASE banking_system;
      ```
    - Remember your MySQL username and password!

---

## 2. **Clone the Project**

```bash
git clone https://github.com/SANTHAN-KUMAR/Banking-system.git
cd Banking-system/Project\ files
```
*(if you’re on Windows, use `cd "Project files"`)*
---

## 3. **Configure Database and Email Settings**

- Open `src/main/resources/application.properties` in a text editor.
- Change these lines to match your MySQL setup:
  ```
  spring.datasource.username=your_mysql_username
  spring.datasource.password=your_mysql_password
  ```
- (Optional) **Email sending for OTPs:**  
  - By default, it’s configured for Gmail SMTP.
  - For local testing, you can ignore errors or use a **fake SMTP** service like [Mailtrap](https://mailtrap.io/).

---

## 4. **Run the Application**

### **On Windows**
```bash
./mvnw.cmd spring-boot:run
```
### **On Mac/Linux**
```bash
./mvnw spring-boot:run
```

- The app will download dependencies (first run may take a few minutes).
- If you see `Started BankingSystemApplication`, it’s running!

---

## 5. **Access the App**

- Open your browser and go to:  
  [http://localhost:8080](http://localhost:8080)

---

## 6. **Register or Log In**

- Register as a new user, or use demo credentials (see README).
- If you want to test Admin/Employee features, use the default test users (if present), or promote yourself via the database.

---

## 7. **Troubleshooting — Common Errors**

- **Port 8080 already in use?**
  - Stop other running servers (like Tomcat, another Java app) or change `server.port` in `application.properties`.

- **Database connection error?**
  - Double check your MySQL is running and credentials in `application.properties` are correct.
  - Database `banking_system` must exist.

- **Email errors on registration?**
  - For local demo, you can ignore. Email sending is needed only for OTP verification.

- **Build failures about Java version?**
  - Make sure `java -version` says at least 17.

---

## 8. **How to Contribute (For Beginners)**

- **Don’t know Java?**  
  - Start with `src/main/resources/templates/` — these are HTML files (Thymeleaf) that control the UI.
- Want to change logic?  
  - Explore `src/main/java/com/santhan/banking_system/controller/` for web endpoints.

- **Need help?**  
  - Open a GitHub issue or discussion in this repo!

---

## 9. **Resetting Database (for a clean slate)**

- If you change transaction hash/date logic, or want to wipe all data, run in MySQL:
  ```sql
  DROP DATABASE banking_system;
  CREATE DATABASE banking_system;
  ```
- (Or set `spring.jpa.hibernate.ddl-auto=create` for auto-wipe on restart.)

---

## 10. **Extra Tips**

- **Don’t edit credentials in code for production!** Use environment variables.
- **For Mac/Windows, file paths and commands are case sensitive**—copy carefully.

---


**“This hybrid approach brings the best of blockchain and banking together—security, auditability, and real-world usability.”**
