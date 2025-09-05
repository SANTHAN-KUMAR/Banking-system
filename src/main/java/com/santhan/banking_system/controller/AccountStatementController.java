package com.santhan.banking_system.controller;

import com.santhan.banking_system.model.Account;
import com.santhan.banking_system.model.Transaction;
import com.santhan.banking_system.model.User;
import com.santhan.banking_system.service.AccountService;
import com.santhan.banking_system.service.TransactionService;
import com.santhan.banking_system.service.UserService;
import com.santhan.banking_system.util.StatementCSVGenerator;
import com.santhan.banking_system.util.StatementPDFGenerator; // Import StatementPDFGenerator
import com.itextpdf.text.DocumentException; // Import DocumentException for PDF generation
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
@RequestMapping("/statements")
public class AccountStatementController {

    private final AccountService accountService;
    private final TransactionService transactionService;
    private final UserService userService;

    @Autowired
    public AccountStatementController(AccountService accountService,
                                      TransactionService transactionService,
                                      UserService userService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.userService = userService;
    }

    /**
     * Displays the form to select date range for statement download.
     * Accessible by USER (for their accounts), EMPLOYEE, ADMIN.
     */
    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_EMPLOYEE') or hasRole('ROLE_ADMIN')")
    @GetMapping("/account/{accountId}")
    public String showStatementForm(@PathVariable Long accountId, Model model, RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        Account account;
        try {
            account = accountService.getAccountById(accountId);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Account not found or invalid ID: " + e.getMessage());
            if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER"))) {
                return "redirect:/user/dashboard";
            }
            return "redirect:/error";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred: " + e.getMessage());
            if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER"))) {
                return "redirect:/user/dashboard";
            }
            return "redirect:/error";
        }

        User loggedInUser = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database."));

        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER"))) {
            if (!Objects.equals(account.getUser().getId(), loggedInUser.getId())) {
                redirectAttributes.addFlashAttribute("error", "You are not authorized to view this account's statement.");
                return "redirect:/user/dashboard";
            }
        }

        model.addAttribute("account", account);
        model.addAttribute("accountId", accountId);
        return "statements/statement-form";
    }


    /**
     * Handles the request to download the CSV or PDF statement.
     * Accessible by USER (for their accounts), EMPLOYEE, ADMIN.
     * Added @RequestParam String format to determine the output type.
     */
    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_EMPLOYEE') or hasRole('ROLE_ADMIN')")
    @PostMapping("/account/{accountId}/download")
    public ResponseEntity<ByteArrayResource> downloadStatement(
            @PathVariable Long accountId,
            @RequestParam("startDate") String startDateStr,
            @RequestParam("endDate") String endDateStr,
            @RequestParam("format") String format, // NEW: Parameter to specify format (csv or pdf)
            RedirectAttributes redirectAttributes) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        Account account;
        try {
            account = accountService.getAccountById(accountId);
        } catch (IllegalArgumentException e) {
            System.err.println("Account not found for statement generation: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Account not found for statement generation.");
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("Unexpected error fetching account: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred while fetching account details.");
            return ResponseEntity.internalServerError().build();
        }

        User loggedInUser = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database."));

        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER"))) {
            if (!Objects.equals(account.getUser().getId(), loggedInUser.getId())) {
                redirectAttributes.addFlashAttribute("error", "You are not authorized to download this account's statement.");
                return ResponseEntity.status(403).build(); // Forbidden
            }
        }

        Instant startDate;
        Instant endDate;
        try {
            startDate = LocalDate.parse(startDateStr).atStartOfDay().toInstant(ZoneOffset.UTC);
            endDate = LocalDate.parse(endDateStr).atTime(23, 59, 59, 999_999_999).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            redirectAttributes.addFlashAttribute("error", "Invalid date format. Please use YYYY-MM-DD.");
            return ResponseEntity.badRequest().build();
        }

        if (startDate.isAfter(endDate)) {
            redirectAttributes.addFlashAttribute("error", "Start date cannot be after end date.");
            return ResponseEntity.badRequest().build();
        }

        try {
            List<Transaction> transactions = transactionService.getTransactionsForAccountInDateRange(accountId, startDate, endDate);
            Map<String, Object> statementSummary = transactionService.calculateStatementBalances(accountId, startDate, endDate);

            BigDecimal openingBalance = (BigDecimal) statementSummary.get("openingBalance");
            BigDecimal closingBalance = (BigDecimal) statementSummary.get("closingBalance");
            String ledgerHash = (String) statementSummary.get("ledgerHash");

            Map<String, Object> finalStatementSummary = new HashMap<>();
            finalStatementSummary.put("openingBalance", openingBalance);
            finalStatementSummary.put("closingBalance", closingBalance);
            finalStatementSummary.put("ledgerHash", ledgerHash);

            byte[] fileBytes;
            String filename;
            String contentType;

            if ("csv".equalsIgnoreCase(format)) {
                fileBytes = StatementCSVGenerator.generateCsvStatement(transactions, finalStatementSummary, account.getAccountNumber());
                filename = "account_statement_" + account.getAccountNumber() + "_" + startDateStr + "_to_" + endDateStr + ".csv";
                contentType = "text/csv";
            } else if ("pdf".equalsIgnoreCase(format)) {
                fileBytes = StatementPDFGenerator.generatePdfStatement(transactions, finalStatementSummary, account.getAccountNumber());
                filename = "account_statement_" + account.getAccountNumber() + "_" + startDateStr + "_to_" + endDateStr + ".pdf";
                contentType = "application/pdf";
            } else {
                redirectAttributes.addFlashAttribute("error", "Invalid statement format requested.");
                return ResponseEntity.badRequest().build();
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(new ByteArrayResource(fileBytes));

        } catch (IOException e) {
            System.err.println("Error generating statement (IO): " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to generate statement due to an internal I/O error.");
            return ResponseEntity.internalServerError().build();
        } catch (DocumentException e) { // Catch DocumentException for PDF errors
            System.err.println("Error generating PDF statement: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to generate PDF statement.");
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            System.err.println("Unexpected error during statement download: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred during statement generation.");
            return ResponseEntity.internalServerError().build();
        }
    }
}
