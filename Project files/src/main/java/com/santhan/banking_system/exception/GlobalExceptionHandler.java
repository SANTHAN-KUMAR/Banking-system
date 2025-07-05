package com.santhan.banking_system.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the banking system.
 * Provides consistent error handling across all controllers.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles business logic exceptions
     */
    @ExceptionHandler(BusinessException.class)
    public String handleBusinessException(BusinessException ex, 
                                         RedirectAttributes redirectAttributes,
                                         HttpServletRequest request) {
        logger.warn("Business exception occurred: {} (Error Code: {})", ex.getMessage(), ex.getErrorCode());
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        
        // Redirect back to the referring page or dashboard
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            return "redirect:" + referer;
        }
        return "redirect:/dashboard";
    }

    /**
     * Handles insufficient funds exceptions
     */
    @ExceptionHandler(InsufficientFundsException.class)
    public String handleInsufficientFundsException(InsufficientFundsException ex,
                                                   RedirectAttributes redirectAttributes,
                                                   HttpServletRequest request) {
        logger.warn("Insufficient funds: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("error", "Insufficient funds: " + ex.getMessage());
        
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            return "redirect:" + referer;
        }
        return "redirect:/dashboard";
    }

    /**
     * Handles resource not found exceptions
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleResourceNotFoundException(ResourceNotFoundException ex,
                                                 RedirectAttributes redirectAttributes,
                                                 HttpServletRequest request) {
        logger.warn("Resource not found: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("error", "Resource not found: " + ex.getMessage());
        
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            return "redirect:" + referer;
        }
        return "redirect:/dashboard";
    }

    /**
     * Handles validation exceptions
     */
    @ExceptionHandler(ValidationException.class)
    public String handleValidationException(ValidationException ex,
                                           RedirectAttributes redirectAttributes,
                                           HttpServletRequest request) {
        logger.warn("Validation error: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("error", "Validation error: " + ex.getMessage());
        
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            return "redirect:" + referer;
        }
        return "redirect:/dashboard";
    }

    /**
     * Handles access denied exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDeniedException(AccessDeniedException ex,
                                             RedirectAttributes redirectAttributes) {
        logger.warn("Access denied: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("error", "Access denied. You don't have permission to perform this action.");
        return "redirect:/dashboard";
    }

    /**
     * Handles method argument validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex) {
        
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Failed");
        response.put("errors", errors);
        
        logger.warn("Validation failed: {}", errors);
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgumentException(IllegalArgumentException ex,
                                                RedirectAttributes redirectAttributes,
                                                HttpServletRequest request) {
        logger.warn("Illegal argument: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            return "redirect:" + referer;
        }
        return "redirect:/dashboard";
    }

    /**
     * Handles general runtime exceptions
     */
    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeException(RuntimeException ex,
                                        RedirectAttributes redirectAttributes,
                                        HttpServletRequest request) {
        logger.error("Unexpected runtime exception occurred", ex);
        redirectAttributes.addFlashAttribute("error", "An unexpected error occurred. Please try again.");
        
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            return "redirect:" + referer;
        }
        return "redirect:/dashboard";
    }

    /**
     * Handles all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception ex,
                                        RedirectAttributes redirectAttributes,
                                        HttpServletRequest request) {
        logger.error("Unexpected exception occurred", ex);
        redirectAttributes.addFlashAttribute("error", "An internal error occurred. Please contact support.");
        
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            return "redirect:" + referer;
        }
        return "redirect:/dashboard";
    }
}