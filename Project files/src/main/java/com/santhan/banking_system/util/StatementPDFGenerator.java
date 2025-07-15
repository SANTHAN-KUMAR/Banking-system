package com.santhan.banking_system.util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.santhan.banking_system.model.Transaction;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

public class StatementPDFGenerator {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public static byte[] generatePdfStatement(
            List<Transaction> transactions,
            Map<String, Object> statementSummary,
            String accountNumber // Account number as String
    ) throws IOException, DocumentException { // Added DocumentException
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4); // Use A4 page size
        PdfWriter.getInstance(document, baos);

        document.open();

        // Define Fonts
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.BLACK);
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.DARK_GRAY);
        Font bodyFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);
        Font boldBodyFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.BLACK);

        // Add Title
        Paragraph title = new Paragraph("Account Statement", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10f);
        document.add(title);

        // Add Account Info and Summary
        Paragraph accountInfo = new Paragraph("Account Number: " + accountNumber, headerFont);
        accountInfo.setSpacingAfter(5f);
        document.add(accountInfo);

        Paragraph periodInfo = new Paragraph(
                "Statement Period: " +
                        (transactions.isEmpty() ? "N/A" : DATE_TIME_FORMATTER.format(transactions.get(0).getTransactionDate())) +
                        " to " +
                        (transactions.isEmpty() ? "N/A" : DATE_TIME_FORMATTER.format(transactions.get(transactions.size() - 1).getTransactionDate())),
                bodyFont
        );
        periodInfo.setSpacingAfter(5f);
        document.add(periodInfo);

        document.add(new Paragraph("Opening Balance: " + ((BigDecimal) statementSummary.get("openingBalance")).setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString(), boldBodyFont));
        document.add(new Paragraph("Closing Balance: " + ((BigDecimal) statementSummary.get("closingBalance")).setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString(), boldBodyFont));
        document.add(new Paragraph("Ledger Hash (Statement Period): " + statementSummary.get("ledgerHash"), bodyFont));
        document.add(new Paragraph("\n")); // Add some space

        // Add Transactions Table
        PdfPTable table = new PdfPTable(8); // 8 columns for transaction details
        table.setWidthPercentage(100); // Table spans 100% of page width
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);
        table.setWidths(new float[]{0.8f, 1f, 1f, 2f, 1.5f, 1.5f, 1.5f, 1f}); // Relative widths

        // Table Headers
        String[] headers = {"ID", "Type", "Amount", "Description", "Date", "Source Acc.", "Dest. Acc.", "Status"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell.setPadding(5);
            table.addCell(cell);
        }
        table.setHeaderRows(1); // Indicate that the first row is the header

        // Add Transaction Rows
        for (Transaction transaction : transactions) {
            table.addCell(new Phrase(String.valueOf(transaction.getId()), bodyFont));
            table.addCell(new Phrase(transaction.getTransactionType().name(), bodyFont));
            table.addCell(new Phrase(transaction.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString(), bodyFont)); // Format BigDecimal
            table.addCell(new Phrase(transaction.getDescription(), bodyFont));
            table.addCell(new Phrase(DATE_TIME_FORMATTER.format(transaction.getTransactionDate()), bodyFont));

            // Safely get account numbers
            String sourceAccountNum = (transaction.getSourceAccount() != null) ? transaction.getSourceAccount().getAccountNumber() : "N/A";
            String destAccountNum = (transaction.getDestinationAccount() != null) ? transaction.getDestinationAccount().getAccountNumber() : "N/A";

            table.addCell(new Phrase(sourceAccountNum, bodyFont));
            table.addCell(new Phrase(destAccountNum, bodyFont));
            table.addCell(new Phrase(transaction.getStatus(), bodyFont));
        }

        document.add(table);

        document.close();
        return baos.toByteArray();
    }
}
