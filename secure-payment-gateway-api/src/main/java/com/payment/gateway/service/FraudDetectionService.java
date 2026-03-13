package com.payment.gateway.service;

import com.payment.gateway.dto.FraudRequest;
import com.payment.gateway.dto.FraudResponse;
import com.payment.gateway.dto.PaymentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class FraudDetectionService {
    private final WebClient webClient;

    public FraudDetectionService(WebClient.Builder webClientBuilder,
                                 @Value("${ml.engine.url}") String mlEngineUrl) {
        this.webClient = webClientBuilder.baseUrl(mlEngineUrl).build();
    }

    public Mono<PaymentResponse> analyzeTransaction(FraudRequest request) {
        return this.webClient.post()
                .uri("/predict")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(FraudResponse.class)
                .onErrorReturn(new FraudResponse("Error", 0.99))
                .map(fraudResponse -> {

                    double risk = fraudResponse.probabilite_fraude();

                    if (risk >= 0.80) {
                        return new PaymentResponse("REJECTED", risk, "Transaction bloquée : Risque de fraude trop élevé.");
                    } else {
                        return new PaymentResponse("APPROVED", risk, "Transaction autorisée.");
                    }
                });
    }

}
