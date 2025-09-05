package com.santhan.banking_system.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.math.RoundingMode; // NEW: Import RoundingMode
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class HashUtil {

    // Define a consistent date/time formatter for Instant, forcing UTC and seconds precision.
    private static final DateTimeFormatter INSTANT_FORMATTER_UTC = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            .withZone(ZoneOffset.UTC);

    // Define a consistent delimiter for hashing components
    private static final String HASH_DELIMITER = "|";


    /**
     * Generates a SHA-256 hash for a given input string.
     * @param input The string to hash.
     * @return The SHA-256 hash as a hexadecimal string.
     * @throws RuntimeException if SHA-256 algorithm is not available (highly unlikely).
     */
    public static String calculateSHA256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found.", e);
        }
    }

    /**
     * Helper method to generate a consistent string representation of a Transaction's core data
     * that will be used as input for hashing.
     * Each piece of data is now separated by a distinct delimiter to prevent concatenation ambiguities.
     *
     * @param id The transaction's ID.
     * @param type The transaction's type.
     * @param amount The transaction amount.
     * @param description The transaction description.
     * @param sourceAccountId The ID of the source account.
     * @param destinationAccountId The ID of the destination account.
     * @param transactionDate The timestamp of the transaction (Instant).
     * @return A concatenated string of transaction data for hashing.
     */
    public static String generateTransactionDataString(
            Long id, com.santhan.banking_system.model.TransactionType type, BigDecimal amount, String description,
            Long sourceAccountId, Long destinationAccountId, Instant transactionDate) {

        // --- CRITICAL FIX: Ensure BigDecimal amount has consistent scale ---
        String amountString = "";
        if (amount != null) {
            // Set scale to 2 (for currency) and use RoundingMode.HALF_UP (common for financial)
            // This ensures "2000" becomes "2000.00" consistently.
            amountString = amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
        }

        // Truncate Instant to seconds before formatting for consistent date string
        String dateString = "";
        if (transactionDate != null) {
            Instant truncatedInstant = transactionDate.truncatedTo(ChronoUnit.SECONDS);
            dateString = INSTANT_FORMATTER_UTC.format(truncatedInstant);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(id != null ? id : "").append(HASH_DELIMITER);
        sb.append(type != null ? type.name() : "").append(HASH_DELIMITER);
        sb.append(amountString).append(HASH_DELIMITER); // Use the formatted amount string
        sb.append(description != null ? description : "").append(HASH_DELIMITER);
        sb.append(sourceAccountId != null ? String.valueOf(sourceAccountId) : "").append(HASH_DELIMITER);
        sb.append(destinationAccountId != null ? String.valueOf(destinationAccountId) : "").append(HASH_DELIMITER);
        sb.append(dateString);

        return sb.toString();
    }
}
