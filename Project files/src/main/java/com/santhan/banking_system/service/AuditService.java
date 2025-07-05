package com.santhan.banking_system.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service for audit logging of banking operations.
 * Provides centralized logging for security and compliance monitoring.
 */
@Service
public class AuditService {

    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    /**
     * Log a banking transaction operation
     */
    public void logTransaction(String operation, Long accountId, BigDecimal amount, String result) {
        String username = getCurrentUsername();
        auditLogger.info("TRANSACTION | User: {} | Operation: {} | Account: {} | Amount: {} | Result: {} | Timestamp: {}",
                username, operation, accountId, amount, result, LocalDateTime.now());
    }

    /**
     * Log a banking transaction operation with source and destination accounts
     */
    public void logTransfer(Long sourceAccountId, Long destinationAccountId, BigDecimal amount, String result) {
        String username = getCurrentUsername();
        auditLogger.info("TRANSFER | User: {} | Source: {} | Destination: {} | Amount: {} | Result: {} | Timestamp: {}",
                username, sourceAccountId, destinationAccountId, amount, result, LocalDateTime.now());
    }

    /**
     * Log user authentication events
     */
    public void logAuthentication(String username, String event, String result) {
        auditLogger.info("AUTH | User: {} | Event: {} | Result: {} | Timestamp: {}",
                username, event, result, LocalDateTime.now());
    }

    /**
     * Log administrative operations
     */
    public void logAdminOperation(String operation, String target, String result) {
        String username = getCurrentUsername();
        auditLogger.info("ADMIN | User: {} | Operation: {} | Target: {} | Result: {} | Timestamp: {}",
                username, operation, target, result, LocalDateTime.now());
    }

    /**
     * Log security events
     */
    public void logSecurityEvent(String event, String details) {
        String username = getCurrentUsername();
        auditLogger.warn("SECURITY | User: {} | Event: {} | Details: {} | Timestamp: {}",
                username, event, details, LocalDateTime.now());
    }

    /**
     * Log fraud-related events
     */
    public void logFraudAlert(String alertType, Long transactionId, String details) {
        String username = getCurrentUsername();
        auditLogger.warn("FRAUD | User: {} | Alert: {} | Transaction: {} | Details: {} | Timestamp: {}",
                username, alertType, transactionId, details, LocalDateTime.now());
    }

    /**
     * Log account management operations
     */
    public void logAccountOperation(String operation, Long accountId, String result) {
        String username = getCurrentUsername();
        auditLogger.info("ACCOUNT | User: {} | Operation: {} | Account: {} | Result: {} | Timestamp: {}",
                username, operation, accountId, result, LocalDateTime.now());
    }

    /**
     * Get the current authenticated username, or "anonymous" if not authenticated
     */
    private String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                !"anonymousUser".equals(authentication.getName())) {
                return authentication.getName();
            }
        } catch (Exception e) {
            logger.debug("Could not get current username", e);
        }
        return "anonymous";
    }
}