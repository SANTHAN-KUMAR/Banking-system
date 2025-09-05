package com.santhan.banking_system.repository;

import com.santhan.banking_system.model.FraudAlert;
import com.santhan.banking_system.model.FraudAlert.AlertStatus;
import com.santhan.banking_system.model.FraudAlert.AlertType;
import com.santhan.banking_system.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {
    List<FraudAlert> findByStatusOrderByCreatedAtDesc(AlertStatus status);
    List<FraudAlert> findAllByOrderByCreatedAtDesc();

    // Fix for: Cannot resolve method 'findByTransactionAndAlertType'
    Optional<FraudAlert> findByTransactionAndAlertType(Transaction transaction, AlertType alertType);

    // Keep this if you still use it elsewhere, otherwise it can be removed
    Optional<FraudAlert> findByTransaction_Id(Long transactionId);
}
