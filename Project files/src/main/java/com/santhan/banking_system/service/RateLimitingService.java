package com.santhan.banking_system.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Simple rate limiting service for banking operations.
 * Prevents abuse of sensitive operations like transfers and withdrawals.
 */
@Service
public class RateLimitingService {

    // Store rate limiting data: key -> last request time and count
    private final ConcurrentMap<String, RateLimitData> rateLimitData = new ConcurrentHashMap<>();

    // Configuration constants
    private static final int MAX_TRANSACTIONS_PER_MINUTE = 10;
    private static final int MAX_LOGIN_ATTEMPTS_PER_MINUTE = 5;
    private static final int MAX_FAILED_LOGINS_PER_HOUR = 10;

    /**
     * Check if a transaction operation is allowed for the given user
     */
    public boolean isTransactionAllowed(String username) {
        return isOperationAllowed(username + ":transaction", MAX_TRANSACTIONS_PER_MINUTE, 1);
    }

    /**
     * Check if a login attempt is allowed for the given username
     */
    public boolean isLoginAttemptAllowed(String username) {
        return isOperationAllowed(username + ":login", MAX_LOGIN_ATTEMPTS_PER_MINUTE, 1);
    }

    /**
     * Check if login is allowed based on failed attempts
     */
    public boolean isLoginAllowedAfterFailures(String username) {
        return isOperationAllowed(username + ":failed_login", MAX_FAILED_LOGINS_PER_HOUR, 60);
    }

    /**
     * Record a failed login attempt
     */
    public void recordFailedLogin(String username) {
        recordOperation(username + ":failed_login", 60);
    }

    /**
     * Clear failed login attempts for a user (on successful login)
     */
    public void clearFailedLogins(String username) {
        rateLimitData.remove(username + ":failed_login");
    }

    /**
     * Generic rate limiting check
     */
    private boolean isOperationAllowed(String key, int maxOperations, int timeWindowMinutes) {
        LocalDateTime now = LocalDateTime.now();
        RateLimitData data = rateLimitData.computeIfAbsent(key, k -> new RateLimitData());

        synchronized (data) {
            // Reset if time window has passed
            if (data.windowStart == null || 
                ChronoUnit.MINUTES.between(data.windowStart, now) >= timeWindowMinutes) {
                data.windowStart = now;
                data.operationCount = 0;
            }

            // Check if limit exceeded
            if (data.operationCount >= maxOperations) {
                return false;
            }

            // Allow operation and increment counter
            data.operationCount++;
            return true;
        }
    }

    /**
     * Record an operation (for tracking purposes)
     */
    private void recordOperation(String key, int timeWindowMinutes) {
        LocalDateTime now = LocalDateTime.now();
        RateLimitData data = rateLimitData.computeIfAbsent(key, k -> new RateLimitData());

        synchronized (data) {
            // Reset if time window has passed
            if (data.windowStart == null || 
                ChronoUnit.MINUTES.between(data.windowStart, now) >= timeWindowMinutes) {
                data.windowStart = now;
                data.operationCount = 0;
            }

            data.operationCount++;
        }
    }

    /**
     * Get remaining operations allowed for a user
     */
    public int getRemainingTransactions(String username) {
        String key = username + ":transaction";
        RateLimitData data = rateLimitData.get(key);
        if (data == null) {
            return MAX_TRANSACTIONS_PER_MINUTE;
        }

        synchronized (data) {
            LocalDateTime now = LocalDateTime.now();
            if (data.windowStart == null || 
                ChronoUnit.MINUTES.between(data.windowStart, now) >= 1) {
                return MAX_TRANSACTIONS_PER_MINUTE;
            }
            return Math.max(0, MAX_TRANSACTIONS_PER_MINUTE - data.operationCount);
        }
    }

    /**
     * Clean up old rate limit data (should be called periodically)
     */
    public void cleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(2);
        rateLimitData.entrySet().removeIf(entry -> {
            RateLimitData data = entry.getValue();
            synchronized (data) {
                return data.windowStart != null && data.windowStart.isBefore(cutoff);
            }
        });
    }

    /**
     * Internal data structure for rate limiting
     */
    private static class RateLimitData {
        LocalDateTime windowStart;
        int operationCount;
    }
}