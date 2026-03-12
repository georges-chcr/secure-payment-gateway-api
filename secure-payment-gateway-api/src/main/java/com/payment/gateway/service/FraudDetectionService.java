package com.payment.gateway.service;

import com.payment.gateway.dto.FraudRequest;
import com.payment.gateway.dto.FraudResponse;
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

    public Mono<FraudResponse> analyzeTransaction(FraudRequest request) {
        return this.webClient.post()
                .uri("/predict")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(FraudResponse.class);
    }

}
