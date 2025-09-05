package com.santhan.banking_system;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

@SpringBootTest
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
    org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration.class,
    org.springframework.boot.autoconfigure.mail.MailSenderValidatorAutoConfiguration.class
})
class BankingSystemApplicationTests {

    @Test
    void contextLoads() {
        // This test simply ensures that the Spring application context loads successfully.
    }
}
