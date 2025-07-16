package com.santhan.banking_system.service;

import com.santhan.banking_system.model.Otp;
import com.santhan.banking_system.model.User;
import com.santhan.banking_system.repository.OtpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.PessimisticLockingFailureException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpRepository otpRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private OtpService otpService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        lenient().doNothing().when(otpRepository).invalidateOtherActiveOtps(anyLong(), any(Otp.OtpPurpose.class), anyLong(), any(LocalDateTime.class));
        lenient().doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void testGenerateAndSendOtp_Success() {
        // Given
        Otp mockOtp = new Otp();
        mockOtp.setId(1L);
        when(otpRepository.save(any(Otp.class))).thenAnswer(invocation -> {
            Otp savedOtp = invocation.getArgument(0);
            savedOtp.setId(1L);
            return savedOtp;
        });

        // When
        String result = otpService.generateAndSendOtp(testUser, Otp.OtpPurpose.EMAIL_VERIFICATION);

        // Then
        assertNotNull(result);
        assertEquals(6, result.length());
        assertTrue(result.matches("\\d{6}"));
        verify(otpRepository).save(any(Otp.class));
        verify(otpRepository).invalidateOtherActiveOtps(eq(1L), eq(Otp.OtpPurpose.EMAIL_VERIFICATION), eq(1L), any(LocalDateTime.class));
        verify(emailService).sendEmail(eq("test@example.com"), anyString(), anyString());
    }

    @Test
    void testGenerateAndSendOtp_RetryOnLockTimeout() {
        // Given
        when(otpRepository.save(any(Otp.class)))
            .thenThrow(new PessimisticLockingFailureException("Lock wait timeout exceeded"))
            .thenAnswer(invocation -> {
                Otp savedOtp = invocation.getArgument(0);
                savedOtp.setId(1L);
                return savedOtp;
            }); // Second attempt succeeds

        // When
        String result = otpService.generateAndSendOtp(testUser, Otp.OtpPurpose.EMAIL_VERIFICATION);

        // Then
        assertNotNull(result);
        assertEquals(6, result.length());
        assertTrue(result.matches("\\d{6}"));
        verify(otpRepository, times(2)).save(any(Otp.class)); // Should be called twice due to retry
        verify(otpRepository).invalidateOtherActiveOtps(eq(1L), eq(Otp.OtpPurpose.EMAIL_VERIFICATION), eq(1L), any(LocalDateTime.class));
        verify(emailService).sendEmail(eq("test@example.com"), anyString(), anyString());
    }

    @Test
    void testGenerateAndSendOtp_FailsAfterMaxRetries() {
        // Given
        when(otpRepository.save(any(Otp.class)))
            .thenThrow(new PessimisticLockingFailureException("Lock wait timeout exceeded"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            otpService.generateAndSendOtp(testUser, Otp.OtpPurpose.EMAIL_VERIFICATION);
        });

        assertTrue(exception.getMessage().contains("Failed to generate OTP after 3 attempts"));
        verify(otpRepository, times(3)).save(any(Otp.class)); // Should be called 3 times (max retries)
        verify(otpRepository, never()).invalidateOtherActiveOtps(anyLong(), any(Otp.OtpPurpose.class), anyLong(), any(LocalDateTime.class));
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void testGenerateAndSendOtp_InvalidUser() {
        // Given
        User invalidUser = new User();
        invalidUser.setEmail(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            otpService.generateAndSendOtp(invalidUser, Otp.OtpPurpose.EMAIL_VERIFICATION);
        });

        assertEquals("User and user's email must not be null for OTP generation.", exception.getMessage());
        verify(otpRepository, never()).save(any(Otp.class));
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }
}