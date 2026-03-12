package com.payment.gateway.controller;

import com.payment.gateway.dto.FraudRequest;
import com.payment.gateway.dto.FraudResponse;
import com.payment.gateway.service.FraudDetectionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final FraudDetectionService fraudService;
    public PaymentController(FraudDetectionService fraudService) {
        this.fraudService = fraudService;
    }

    @PostMapping("/process")
    public Mono<FraudResponse> processPayment(@RequestBody FraudRequest request) {
        return fraudService.analyzeTransaction(request);
    }

}
