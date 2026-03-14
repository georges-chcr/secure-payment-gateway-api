package com.payment.gateway.repository;

import com.payment.gateway.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    }