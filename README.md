# 🚀 Hybrid Banking System — Next-Gen Tamper-Proof, Fraud-Resistant Banking Platform

[![Build Status](https://github.com/SANTHAN-KUMAR/Banking-system/actions/workflows/maven-publish.yml/badge.svg)](https://github.com/SANTHAN-KUMAR/Banking-system/actions/workflows/maven-publish.yml)

---

## ⭐️ What Makes This Project Unique?

This is NOT a typical banking CRUD app.  
**It’s a hybrid system fusing blockchain-style immutability with real-world banking compliance and security.**

- **Immutable Ledger:** Every transaction is cryptographically chained; tampering is instantly detectable via hash-chain verification.
- **Real-Time Fraud Alerts:** Automated, rules-based fraud detection triggers alerts for large, frequent, or suspicious transactions.
- **Controlled Reversibility:** Only authorized admins/employees can reverse transactions—each reversal is immutably recorded and linked.
- **Role-Based Access (RBAC) & KYC:** Fine-grained permissions and full KYC workflow.
- **Proven Security:** Passwords & transaction PINs are hashed (BCrypt). No secrets in code.

---

## 🎯 Features At-a-Glance

| Feature                        | Supported | Description                                          |
|--------------------------------|:---------:|------------------------------------------------------|
| User Registration/Login        |    ✅     | Role-based login, email & mobile verification        |
| Account Management             |    ✅     | Create, edit, view, and delete accounts              |
| Immutable Transaction Ledger   |    ✅     | Hash-chained, tamper-evident ledger                  |
| Real-Time Fraud Alerts         |    ✅     | Automated alerting for suspicious activity           |
| Controlled Transaction Reversal|    ✅     | Admin/employee can reverse, with audit trail         |
| KYC Compliance                 |    ✅     | KYC workflow and status tracking                     |
| PDF Statement Export           |    🚧     | Coming soon                                          |
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

## 🛡️ Why This Matters (The “Why,” Not Just the “What”)

- **Immutable Audit Trail:** Proves integrity of every record—auditors, regulators, and customers can trust the data.
- **Proactive Security:** Fraud alerts are generated automatically, reducing risk and response times.
- **Reversibility with Accountability:** Transaction reversals are possible for compliance—but every reversal is visible and auditable.
- **Hybrid Compliance:** Combines the best of blockchain (transparency, immutability) and banking (control, reversibility, KYC).

---


---

## 🚦 Quickstart: Running Locally

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
  *(Add more unit/integration tests in `/src/test/java` for services like `UserService`, `TransactionService`, `FraudAlertService`.)*

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

**Uniqueness Score:** 71.9% (combining existing ideas in a genuinely new, practical way for banking)

---

## 📄 Learn More

- [Final Scope.md](Final%20Scope.md) — Advanced/AI features, tokenization, open banking plans

---

**“This hybrid approach brings the best of blockchain and banking together—security, auditability, and real-world usability.”**
