package com.santhan.banking_system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the banking system.
 * Allows externalized configuration for different environments.
 */
@Configuration
@ConfigurationProperties(prefix = "banking.system")
public class BankingSystemProperties {

    private Security security = new Security();
    private Transaction transaction = new Transaction();
    private Audit audit = new Audit();

    // Getters and Setters
    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public Audit getAudit() {
        return audit;
    }

    public void setAudit(Audit audit) {
        this.audit = audit;
    }

    /**
     * Security-related configuration
     */
    public static class Security {
        private int maxLoginAttempts = 5;
        private int sessionTimeoutMinutes = 30;
        private boolean csrfEnabled = true;

        public int getMaxLoginAttempts() {
            return maxLoginAttempts;
        }

        public void setMaxLoginAttempts(int maxLoginAttempts) {
            this.maxLoginAttempts = maxLoginAttempts;
        }

        public int getSessionTimeoutMinutes() {
            return sessionTimeoutMinutes;
        }

        public void setSessionTimeoutMinutes(int sessionTimeoutMinutes) {
            this.sessionTimeoutMinutes = sessionTimeoutMinutes;
        }

        public boolean isCsrfEnabled() {
            return csrfEnabled;
        }

        public void setCsrfEnabled(boolean csrfEnabled) {
            this.csrfEnabled = csrfEnabled;
        }
    }

    /**
     * Transaction-related configuration
     */
    public static class Transaction {
        private int maxTransactionsPerMinute = 10;
        private String defaultCurrency = "USD";
        private boolean fraudDetectionEnabled = true;

        public int getMaxTransactionsPerMinute() {
            return maxTransactionsPerMinute;
        }

        public void setMaxTransactionsPerMinute(int maxTransactionsPerMinute) {
            this.maxTransactionsPerMinute = maxTransactionsPerMinute;
        }

        public String getDefaultCurrency() {
            return defaultCurrency;
        }

        public void setDefaultCurrency(String defaultCurrency) {
            this.defaultCurrency = defaultCurrency;
        }

        public boolean isFraudDetectionEnabled() {
            return fraudDetectionEnabled;
        }

        public void setFraudDetectionEnabled(boolean fraudDetectionEnabled) {
            this.fraudDetectionEnabled = fraudDetectionEnabled;
        }
    }

    /**
     * Audit-related configuration
     */
    public static class Audit {
        private boolean enabled = true;
        private String logLevel = "INFO";
        private boolean includeRequestDetails = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getLogLevel() {
            return logLevel;
        }

        public void setLogLevel(String logLevel) {
            this.logLevel = logLevel;
        }

        public boolean isIncludeRequestDetails() {
            return includeRequestDetails;
        }

        public void setIncludeRequestDetails(boolean includeRequestDetails) {
            this.includeRequestDetails = includeRequestDetails;
        }
    }
}