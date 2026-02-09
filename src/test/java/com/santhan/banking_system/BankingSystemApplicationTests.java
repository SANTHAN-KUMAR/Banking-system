package com.santhan.banking_system;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles; // Import ActiveProfiles

@SpringBootTest
@ActiveProfiles("test") // Explicitly activate the "test" profile for this test class
class BankingSystemApplicationTests {

    @Test
    void contextLoads() {
        // This test simply ensures that the Spring application context loads successfully.
        // If it reaches this point, the context has loaded.
    }

}
