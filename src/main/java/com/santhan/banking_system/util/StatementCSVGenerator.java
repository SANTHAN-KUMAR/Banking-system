package com.santhan.banking_system.util;

import com.santhan.banking_system.model.Transaction;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

public class StatementCSVGenerator {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault()); // Use system default timezone for display

    public static byte[] generateCsvStatement(
            List<Transaction> transactions,
            Map<String, Object> statementSummary,
            String accountNumber // CHANGED: This is now String to match Account.accountNumber
    ) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos);

        // Header for the statement
        writer.println("Account Statement for Account: " + accountNumber);
        writer.println("Statement Period: " +
                (transactions.isEmpty() ? "N/A" : DATE_TIME_FORMATTER.format(transactions.get(0).getTransactionDate())) +
                " to " +
                (transactions.isEmpty() ? "N/A" : DATE_TIME_FORMATTER.format(transactions.get(transactions.size() - 1).getTransactionDate())));
        writer.println("Opening Balance," + statementSummary.get("openingBalance"));
        writer.println("Closing Balance," + statementSummary.get("closingBalance"));
        writer.println("Ledger Hash (Statement Period)," + statementSummary.get("ledgerHash"));
        writer.println(); // Blank line for separation

        // CSV Header for transactions
        writer.println("Transaction ID,Type,Amount,Description,Transaction Date,Source Account,Destination Account,Transaction Hash,Status");

        // Transaction data
        for (Transaction transaction : transactions) {
            String sourceAccountNum = (transaction.getSourceAccount() != null) ? transaction.getSourceAccount().getAccountNumber() : "N/A";
            String destAccountNum = (transaction.getDestinationAccount() != null) ? transaction.getDestinationAccount().getAccountNumber() : "N/A";

            writer.printf("%d,%s,%.2f,\"%s\",%s,%s,%s,%s,%s%n",
                    transaction.getId(),
                    transaction.getTransactionType().name(),
                    transaction.getAmount(),
                    // Escape double quotes within description if any
                    transaction.getDescription().replace("\"", "\"\""),
                    DATE_TIME_FORMATTER.format(transaction.getTransactionDate()),
                    sourceAccountNum,
                    destAccountNum,
                    transaction.getTransactionHash(),
                    transaction.getStatus()
            );
        }

        writer.flush();
        return baos.toByteArray();
    }
}