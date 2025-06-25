package com.santhan.banking_system.repository; // Or com.santhan.banking_system.fraud.repository

import com.santhan.banking_system.model.FraudAlert; // Import the FraudAlert model
import com.santhan.banking_system.model.FraudAlert.AlertStatus; // Import AlertStatus enum
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {

    // Custom query to find alerts by status
    List<FraudAlert> findByStatus(AlertStatus status);

    // Custom query to find alerts associated with a specific transaction ID
    Optional<FraudAlert> findByTransaction_Id(Long transactionId);

    // Custom query to find alerts created within a specific time range (e.g., for daily reports)
    // List<FraudAlert> findByCreatedAtBetween(Instant start, Instant end);

    // You can add more custom query methods here as needed for your fraud detection logic,
    // e.g., finding alerts for a specific account, or by alert type.
}
