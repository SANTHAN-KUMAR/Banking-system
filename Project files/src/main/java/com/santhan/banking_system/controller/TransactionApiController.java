package com.santhan.banking_system.controller;

import com.santhan.banking_system.dto.TransactionRequest;
import com.santhan.banking_system.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

/**
 * REST API Controller for transaction operations.
 * Provides API endpoints for deposits, withdrawals, and transfers.
 */
@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transaction Operations", description = "APIs for banking transaction operations")
public class TransactionApiController {

    private final TransactionService transactionService;

    @Autowired
    public TransactionApiController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Operation(
        summary = "Make a deposit",
        description = "Deposit money into a specified account"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Deposit successful"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @PostMapping("/deposit")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'CUSTOMER')")
    public ResponseEntity<String> deposit(
        @Parameter(description = "Account ID for the deposit", required = true)
        @RequestParam Long accountId,
        
        @Parameter(description = "Amount to deposit", required = true)
        @RequestParam BigDecimal amount,
        
        @Parameter(description = "Description of the deposit")
        @RequestParam(required = false) String description) {
        
        try {
            transactionService.deposit(accountId, amount, description);
            return ResponseEntity.ok("Deposit successful");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @Operation(
        summary = "Make a withdrawal",
        description = "Withdraw money from a specified account"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Withdrawal successful"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters or insufficient funds"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @PostMapping("/withdraw")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'CUSTOMER')")
    public ResponseEntity<String> withdraw(
        @Parameter(description = "Account ID for the withdrawal", required = true)
        @RequestParam Long accountId,
        
        @Parameter(description = "Amount to withdraw", required = true)
        @RequestParam BigDecimal amount,
        
        @Parameter(description = "Description of the withdrawal")
        @RequestParam(required = false) String description) {
        
        try {
            transactionService.withdraw(accountId, amount, description);
            return ResponseEntity.ok("Withdrawal successful");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @Operation(
        summary = "Transfer money between accounts",
        description = "Transfer money from one account to another"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transfer successful"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters or insufficient funds"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'CUSTOMER')")
    public ResponseEntity<String> transfer(
        @Parameter(description = "Source account ID", required = true)
        @RequestParam Long sourceAccountId,
        
        @Parameter(description = "Destination account ID", required = true)
        @RequestParam Long destinationAccountId,
        
        @Parameter(description = "Amount to transfer", required = true)
        @RequestParam BigDecimal amount,
        
        @Parameter(description = "Description of the transfer")
        @RequestParam(required = false) String description) {
        
        try {
            transactionService.transfer(sourceAccountId, destinationAccountId, amount, description);
            return ResponseEntity.ok("Transfer successful");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @Operation(
        summary = "Make a deposit using request body",
        description = "Deposit money using a structured request body with validation"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Deposit successful"),
        @ApiResponse(responseCode = "400", description = "Validation failed or business logic error"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PostMapping("/deposit-structured")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'CUSTOMER')")
    public ResponseEntity<String> depositStructured(
        @Parameter(description = "Transaction request with validation", required = true)
        @Valid @RequestBody TransactionRequest request) {
        
        try {
            transactionService.deposit(request.getAccountId(), request.getAmount(), request.getDescription());
            return ResponseEntity.ok("Deposit successful");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}