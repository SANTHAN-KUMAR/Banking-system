package com.santhan.banking_system;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser; // NEW import

@SpringBootTest
class BankingSystemApplicationTests {

	@Test
	@WithMockUser
	void contextLoads() {
	}

}
