package com.santhan.banking_system.repository;

import com.santhan.banking_system.model.Account;
import com.santhan.banking_system.model.User; // <--- Make sure User is imported here
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    // Spring Data JPA will automatically implement this method based on its name
    List<Account> findByUser(User user); // <--- THIS METHOD IS CRUCIAL

    // Find an account by its unique account number
    Optional<Account> findByAccountNumber(String accountNumber);
}