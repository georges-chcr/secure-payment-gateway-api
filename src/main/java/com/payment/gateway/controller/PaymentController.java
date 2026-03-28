package com.payment.gateway.controller;

import com.payment.gateway.dto.FraudRequest;
import com.payment.gateway.dto.PaymentResponse;
import com.payment.gateway.model.PaymentTransaction;
import com.payment.gateway.service.FraudDetectionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    // TransactionRepository retiré du Controller (J5/J6) — toute la logique
    // de données passe désormais par la couche service.
    public PaymentController(FraudDetectionService fraudService) {
        this.fraudService = fraudService;
    }

    /**
     * Soumet une transaction pour analyse de fraude.
     * La validation Bean Validation (@Valid) rejette toute requête
     * dont la liste de features n'est pas exactement de taille 30.
     */
    @PostMapping("/process")
    public Mono<PaymentResponse> processPayment(@Valid @RequestBody FraudRequest request) {
        return fraudService.analyzeTransaction(request);
    }

    /**
     * Retourne les 50 dernières transactions sous forme de tableau JSON plat.
     */
    @GetMapping("/history")
    public Mono<List<PaymentTransaction>> getHistory() {
        return fraudService.getTransactionHistory();
    }

    /**
     * Vide l'intégralité de l'historique des transactions.
     * DELETE /api/v1/payments/history → 200 OK (corps vide)
     */
    @DeleteMapping("/history")
    public Mono<Void> clearHistory() {
        return fraudService.clearHistory();
    }
}
