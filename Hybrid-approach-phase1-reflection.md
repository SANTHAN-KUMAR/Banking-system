# Phase 1: Immutable Transaction Ledger (Blockchain-like Audit Trail)

This phase was all about making our banking system's transaction history **tamper-proof** and **auditable**. We implemented a core "blockchain-like" ledger that cryptographically links every transaction.

## How it Works

Every transaction now gets two new fields:

* **`transactionHash`**: A unique digital fingerprint (SHA-256 hash) of its own data.
* **`previousTransactionHash`**: The hash of the transaction that came immediately before it.

This creates a chain. If even a tiny detail of an old transaction is changed in the database, its `transactionHash` will no longer match, and the entire chain breaks, immediately signaling tampering.

## The "Damn Error" (and how we crushed it!)

The biggest headache was a persistent "**TAMPERING DETECTED**" error, even when we hadn't tampered! This was because the string data used to generate the hash was subtly different when the transaction was saved versus when it was recalculated for verification. Even a single character difference results in a completely new hash.

The culprits and fixes were:

* **Date/Time Precision**: Java's precise timestamps didn't always match what the database stored.
    * **Fix**: We forced all transaction dates to be stored and hashed as UTC seconds (using `Instant` and truncating precision).

* **Amount Formatting**: `BigDecimal` amounts like `1000` could be stored as `1000` or `1000.00`, leading to different hash inputs.
    * **Fix**: We forced `BigDecimal` amounts to always have two decimal places (`.00`) before hashing.

* **Ambiguous String Concatenation**: When combining different pieces of data for hashing (e.g., ID, type, amount, description), there were no clear boundaries.
    * **Fix**: We added a strict delimiter `|` between every single data field in the hashing string (e.g., `1|DEPOSIT|1000.00|...`).

**Key takeaway**: Hashing is extremely sensitive. Every byte in the input string must be identical between creation and verification.

## Outcome

After meticulously addressing these consistency issues and rigorously clearing the database with each code change, the ledger integrity check now works perfectly. We can confidently make transactions, verify the chain, and – most importantly – the system successfully detects any tampering with historical records.

---

This wraps up a crucial foundation for our secure banking system!
