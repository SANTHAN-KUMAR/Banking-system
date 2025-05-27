package com.santhan.banking_system.repository;

import com.santhan.banking_system.model.User; // Import your User entity
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository; // Optional but good practice

@Repository // Marks this interface as a Spring Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Spring Data JPA automatically provides methods like:
    // User save(User user);
    // Optional<User> findById(Long id);
    // List<User> findAll();
    // void delete(User user);
    // void deleteById(Long id);

    // You can add custom query methods here if needed, e.g.:
    // Optional<User> findByUsername(String username);
}