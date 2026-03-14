package com.payment.gateway.service;

import com.payment.gateway.dto.FraudRequest;
import com.payment.gateway.dto.FraudResponse;
import com.payment.gateway.dto.PaymentResponse;
import com.payment.gateway.model.PaymentTransaction;
import com.payment.gateway.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class FraudDetectionService {

    private final WebClient webClient;
    private final TransactionRepository transactionRepository;

    public FraudDetectionService(WebClient.Builder webClientBuilder,
                                 @Value("${ml.engine.url}") String mlEngineUrl,
                                 TransactionRepository transactionRepository) {
        this.webClient = webClientBuilder.baseUrl(mlEngineUrl).build();
        this.transactionRepository = transactionRepository;
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
                    String status;
                    String message;

                    if (risk >= 0.80) {
                        status = "REJECTED";
                        message = "Transaction bloquée : Risque de fraude trop élevé.";
                    } else {
                        status = "APPROVED";
                        message = "Transaction autorisée.";
                    }

                    PaymentTransaction transaction = new PaymentTransaction(
                            request.features(),
                            risk,
                            status,
                            message
                    );
                    transactionRepository.save(transaction);

                    return new PaymentResponse(status, risk, message);
                });
    }
}