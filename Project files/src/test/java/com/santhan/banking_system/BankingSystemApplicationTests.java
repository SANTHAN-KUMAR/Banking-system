package com.santhan.banking_system;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser; // NEW import
import org.springframework.test.context.ActiveProfiles; // Add this import

@SpringBootTest
@ActiveProfiles("test") // Add this line
class BankingSystemApplicationTests {

    @Test
    @WithMockUser
    void contextLoads() {
    }
}
