package com.santhan.banking_system.exception;

/**
 * Exception thrown when insufficient funds are available for a banking operation.
 */
public class InsufficientFundsException extends BusinessException {
    
    public InsufficientFundsException(String message) {
        super(message, "INSUFFICIENT_FUNDS");
    }
    
    public InsufficientFundsException(String message, Throwable cause) {
        super(message, "INSUFFICIENT_FUNDS", cause);
    }
}