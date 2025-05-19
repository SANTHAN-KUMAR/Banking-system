package com.santhan.banking_system.controller;

import com.santhan.banking_system.model.Account; // Import Account model
import com.santhan.banking_system.service.AccountService; // Import Account Service
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal; // Import BigDecimal

@RestController
@RequestMapping("/api/accounts") // Base URL path for this controller
public class AccountController {

    private final AccountService accountService; // Declare the Service dependency

    // Constructor Injection
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // --- Example Endpoint: Deposit funds into an account ---
    // We'll send the account number in the path and amount in the request body
    @PostMapping("/{accountNumber}/deposit")
    public ResponseEntity<Account> depositFunds(
            @PathVariable String accountNumber,
            @RequestBody BigDecimal amount) { // Assuming the request body is just the amount

        Account updatedAccount = accountService.deposit(accountNumber, amount); // Call the Service layer

        // Return the updated account details and a 200 (OK) status
        return ResponseEntity.ok(updatedAccount);
    }

    // You'll add endpoints for withdrawal, transfer, get account balance, create account, etc.
}