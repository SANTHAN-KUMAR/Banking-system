package com.santhan.banking_system.service;

import com.santhan.banking_system.model.User;
import com.santhan.banking_system.model.UserRole;
import com.santhan.banking_system.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserRegistrationIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testUserRegistrationWithOtpGeneration() {
        // Given - Create a new user like in the registration scenario
        User newUser = new User();
        newUser.setUsername("testuser123");
        newUser.setEmail("test@example.com");
        newUser.setPassword("TestPassword123");
        newUser.setRole(UserRole.ROLE_CUSTOMER);
        newUser.setFirstName("Test");
        newUser.setLastName("User");

        // When - Create the user (this will trigger OTP generation)
        User createdUser = userService.createUser(newUser);

        // Then - Verify user was created successfully
        assertNotNull(createdUser);
        assertNotNull(createdUser.getId());
        assertEquals("testuser123", createdUser.getUsername());
        assertEquals("test@example.com", createdUser.getEmail());
        assertFalse(createdUser.isEmailVerified()); // Should be false initially
        assertEquals(UserRole.ROLE_CUSTOMER, createdUser.getRole());

        // Verify user exists in database
        User foundUser = userRepository.findById(createdUser.getId()).orElse(null);
        assertNotNull(foundUser);
        assertEquals("testuser123", foundUser.getUsername());
        
        // This test verifies that user creation succeeds even if OTP generation 
        // encounters issues, which is the expected behavior after our fix
    }

    @Test
    public void testMultipleUserRegistrations() {
        // Test scenario where multiple users register in succession
        // This simulates the situation after "Resend OTP" works
        
        for (int i = 1; i <= 3; i++) {
            User newUser = new User();
            newUser.setUsername("user" + i);
            newUser.setEmail("user" + i + "@example.com");
            newUser.setPassword("Password123");
            newUser.setRole(UserRole.ROLE_CUSTOMER);
            newUser.setFirstName("User");
            newUser.setLastName("Number" + i);

            User createdUser = userService.createUser(newUser);
            
            assertNotNull(createdUser);
            assertNotNull(createdUser.getId());
            assertEquals("user" + i, createdUser.getUsername());
            assertEquals("user" + i + "@example.com", createdUser.getEmail());
        }
        
        // All users should be created successfully
        assertEquals(3, userRepository.count());
    }
}