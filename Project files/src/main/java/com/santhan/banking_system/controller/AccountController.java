// src/main/java/com/santhan/banking_system/controller/AccountController.java

package com.santhan.banking_system.controller;

import com.santhan.banking_system.model.Account;
import com.santhan.banking_system.model.User;
import com.santhan.banking_system.service.AccountService;
import com.santhan.banking_system.service.UserService;
import com.santhan.banking_system.service.TransactionService;
import com.santhan.banking_system.model.AccountType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;
    private final UserService userService;
    private final TransactionService transactionService;

    @Autowired
    public AccountController(AccountService accountService, UserService userService, TransactionService transactionService) {
        this.accountService = accountService;
        this.userService = userService;
        this.transactionService = transactionService;
    }

    @GetMapping({"", "/list"})
    @Transactional
    public String listAccounts(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User currentUser = userService.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("Logged-in user not found!"));

        List<Account> accounts = accountService.getAccountsByUserId(currentUser.getId());
        model.addAttribute("accounts", accounts);
        model.addAttribute("username", currentUsername);

        if (model.containsAttribute("success")) {
            model.addAttribute("success", model.getAttribute("success"));
        }
        if (model.containsAttribute("error")) {
            model.addAttribute("error", model.getAttribute("error"));
        }

        return "account-list";
    }

    @GetMapping("/create")
    public String showCreateAccountForm(Model model) {
        model.addAttribute("account", new Account());
        model.addAttribute("allAccountTypes", AccountType.values());
        return "account-create";
    }

    @PostMapping("/create")
    @Transactional
    public String createAccount(@ModelAttribute("account") Account account, RedirectAttributes redirectAttributes) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();
            User currentUser = userService.findByUsername(currentUsername)
                    .orElseThrow(() -> new IllegalArgumentException("Logged-in user not found!"));

            accountService.createAccount(currentUser.getId(), account);
            redirectAttributes.addFlashAttribute("success", "Account created successfully!");
            return "redirect:/accounts/list";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating account: " + e.getMessage());
            return "redirect:/accounts/create";
        }
    }

    @GetMapping("/details/{id}")
    @Transactional
    public String showAccountDetails(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();
            User currentUser = userService.findByUsername(currentUsername)
                    .orElseThrow(() -> new IllegalArgumentException("Logged-in user not found!"));

            Account account = accountService.getAccountById(id);

            if (!account.getUser().getId().equals(currentUser.getId())) {
                redirectAttributes.addFlashAttribute("error", "You do not have permission to view this account.");
                return "redirect:/accounts/list";
            }

            model.addAttribute("account", account);
            return "account-details";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/accounts/list";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred: " + e.getMessage());
            return "redirect:/accounts/list";
        }
    }

    @GetMapping("/{accountId}/deposit")
    public String showDepositForm(@PathVariable Long accountId, Model model, RedirectAttributes redirectAttributes) {
        try {
            Account account = accountService.getAccountById(accountId);
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();
            User currentUser = userService.findByUsername(currentUsername)
                    .orElseThrow(() -> new IllegalArgumentException("Logged-in user not found!"));

            if (!account.getUser().getId().equals(currentUser.getId())) {
                redirectAttributes.addFlashAttribute("error", "You do not have permission to deposit into this account.");
                return "redirect:/accounts/list";
            }
            model.addAttribute("account", account);
            return "deposit";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/accounts/list";
        }
    }

    @PostMapping("/{accountId}/deposit")
    public String processDeposit(@PathVariable Long accountId, @RequestParam BigDecimal amount,
                                 @RequestParam(required = false) String description,
                                 RedirectAttributes redirectAttributes) {
        try {
            Account account = accountService.getAccountById(accountId);
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();
            User currentUser = userService.findByUsername(currentUsername)
                    .orElseThrow(() -> new IllegalArgumentException("Logged-in user not found!"));
            if (!account.getUser().getId().equals(currentUser.getId())) {
                redirectAttributes.addFlashAttribute("error", "You do not have permission to deposit into this account.");
                return "redirect:/accounts/list";
            }

            transactionService.deposit(accountId, amount, description);
            redirectAttributes.addFlashAttribute("success", "Deposit successful!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounts/details/" + accountId;
    }

    @GetMapping("/{accountId}/withdraw")
    public String showWithdrawForm(@PathVariable Long accountId, Model model, RedirectAttributes redirectAttributes) {
        try {
            Account account = accountService.getAccountById(accountId);
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();
            User currentUser = userService.findByUsername(currentUsername)
                    .orElseThrow(() -> new IllegalArgumentException("Logged-in user not found!"));

            if (!account.getUser().getId().equals(currentUser.getId())) {
                redirectAttributes.addFlashAttribute("error", "You do not have permission to withdraw from this account.");
                return "redirect:/accounts/list";
            }
            model.addAttribute("account", account);
            return "withdraw";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/accounts/list";
        }
    }

    @PostMapping("/{accountId}/withdraw")
    public String processWithdraw(@PathVariable Long accountId, @RequestParam BigDecimal amount,
                                  @RequestParam(required = false) String description,
                                  RedirectAttributes redirectAttributes) {
        try {
            Account account = accountService.getAccountById(accountId);
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();
            User currentUser = userService.findByUsername(currentUsername)
                    .orElseThrow(() -> new IllegalArgumentException("Logged-in user not found!"));
            if (!account.getUser().getId().equals(currentUser.getId())) {
                redirectAttributes.addFlashAttribute("error", "You do not have permission to withdraw from this account.");
                return "redirect:/accounts/list";
            }

            transactionService.withdraw(accountId, amount, description);
            redirectAttributes.addFlashAttribute("success", "Withdrawal successful!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounts/details/" + accountId;
    }


    // --- TRANSFER FUNCTIONALITY ---

    @GetMapping("/{sourceAccountId}/transfer")
    @Transactional
    public String showTransferForm(@PathVariable Long sourceAccountId, Model model, RedirectAttributes redirectAttributes) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();
            User currentUser = userService.findByUsername(currentUsername)
                    .orElseThrow(() -> new IllegalArgumentException("Logged-in user not found!"));

            Account sourceAccount = accountService.getAccountById(sourceAccountId);
            if (!sourceAccount.getUser().getId().equals(currentUser.getId())) {
                redirectAttributes.addFlashAttribute("error", "You do not have permission to transfer from this account.");
                return "redirect:/accounts/list";
            }

            List<Account> allAccounts = accountService.getAllAccounts();
            allAccounts.forEach(account -> {
                if (account.getUser() != null) {
                    account.getUser().getUsername();
                }
            });
            allAccounts.removeIf(account -> account.getId().equals(sourceAccountId));


            model.addAttribute("sourceAccount", sourceAccount);
            model.addAttribute("allAccounts", allAccounts);
            return "transfer";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/accounts/list";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred: " + e.getMessage());
            return "redirect:/accounts/list";
        }
    }

    @PostMapping("/{sourceAccountId}/transfer")
    public String processTransfer(@PathVariable Long sourceAccountId,
                                  @RequestParam Long destinationAccountId,
                                  @RequestParam BigDecimal amount,
                                  @RequestParam(required = false) String description,
                                  RedirectAttributes redirectAttributes) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();
            User currentUser = userService.findByUsername(currentUsername)
                    .orElseThrow(() -> new IllegalArgumentException("Logged-in user not found!"));

            Account sourceAccount = accountService.getAccountById(sourceAccountId);
            if (!sourceAccount.getUser().getId().equals(currentUser.getId())) {
                redirectAttributes.addFlashAttribute("error", "You do not have permission to transfer from this account.");
                return "redirect:/accounts/list";
            }

            transactionService.transfer(sourceAccountId, destinationAccountId, amount, description);
            redirectAttributes.addFlashAttribute("success", "Transfer successful!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An error occurred during transfer: " + e.getMessage());
        }
        return "redirect:/accounts/details/" + sourceAccountId;
    }

    // --- TRANSACTION HISTORY VIEW (NEW/CONFIRMED) ---
    @GetMapping("/{accountId}/transactions")
    @Transactional
    public String viewAccountTransactions(@PathVariable Long accountId, Model model, RedirectAttributes redirectAttributes) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();
            User currentUser = userService.findByUsername(currentUsername)
                    .orElseThrow(() -> new IllegalArgumentException("Logged-in user not found!"));

            Account account = accountService.getAccountById(accountId);

            if (!account.getUser().getId().equals(currentUser.getId())) {
                redirectAttributes.addFlashAttribute("error", "You do not have permission to view transactions for this account.");
                return "redirect:/accounts/list";
            }

            List<com.santhan.banking_system.model.Transaction> transactions = transactionService.getTransactionsForAccount(accountId);
            transactions.forEach(transaction -> {
                if (transaction.getSourceAccount() != null && transaction.getSourceAccount().getUser() != null) {
                    transaction.getSourceAccount().getUser().getUsername();
                }
                if (transaction.getDestinationAccount() != null && transaction.getDestinationAccount().getUser() != null) {
                    transaction.getDestinationAccount().getUser().getUsername();
                }
            });


            model.addAttribute("account", account);
            model.addAttribute("transactions", transactions);
            return "account-transactions"; // This template needs to be created
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/accounts/list";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred: " + e.getMessage());
            return "redirect:/accounts/list";
        }
    }
}
