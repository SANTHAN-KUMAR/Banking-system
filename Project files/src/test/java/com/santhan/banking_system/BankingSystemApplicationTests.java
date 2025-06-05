package com.santhan.banking_system;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser; // NEW import

@SpringBootTest
@ActiveProfiles("test") // Add this line
class BankingSystemApplicationTests {

	@Test
	@WithMockUser
	void contextLoads() {
	}

}
