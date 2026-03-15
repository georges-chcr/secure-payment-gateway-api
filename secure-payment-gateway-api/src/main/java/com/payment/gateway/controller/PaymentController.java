package com.payment.gateway.controller;

import com.payment.gateway.dto.FraudRequest;
import com.payment.gateway.dto.PaymentResponse;
import com.payment.gateway.model.PaymentTransaction;
import com.payment.gateway.repository.TransactionRepository;
import com.payment.gateway.service.FraudDetectionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final FraudDetectionService fraudService;
    private final TransactionRepository transactionRepository;


    public PaymentController(FraudDetectionService fraudService, TransactionRepository transactionRepository) {
        this.fraudService = fraudService;
        this.transactionRepository = transactionRepository;
    }

    @PostMapping("/process")
    public Mono<PaymentResponse> processPayment(@RequestBody FraudRequest request) {
        return fraudService.analyzeTransaction(request);
    }


    @GetMapping("/history")
    public List<PaymentTransaction> getHistory() {
        return transactionRepository.findAll();
    }
}