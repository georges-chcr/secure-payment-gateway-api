package com.payment.gateway.service;

import com.payment.gateway.dto.FraudRequest;
import com.payment.gateway.dto.FraudResponse;
import com.payment.gateway.dto.PaymentResponse;
import com.payment.gateway.model.PaymentTransaction;
import com.payment.gateway.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;

@Service
public class FraudDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(FraudDetectionService.class);

    private final WebClient webClient;
    private final TransactionRepository transactionRepository;

    public FraudDetectionService(WebClient.Builder webClientBuilder,
                                 @Value("${ml.engine.url}") String mlEngineUrl,
                                 TransactionRepository transactionRepository) {
        this.webClient = webClientBuilder.baseUrl(mlEngineUrl).build();
        this.transactionRepository = transactionRepository;
        logger.info("FraudDetectionService initialisé — ML Engine URL : {}", mlEngineUrl);
    }

    /**
     * Envoie les features de transaction au service ML Python et applique
     * la logique métier à 3 niveaux selon le score de risque retourné.
     *
     * Seuils :
     *   risk < 0.70          → APPROVED      (transaction autorisée)
     *   0.70 ≤ risk < 0.85   → MANUAL_REVIEW (revue humaine requise)
     *   risk ≥ 0.85          → REJECTED      (fraude bloquée)
     *
     * Stratégie de failover : fail-closed (risk=0.99 → REJECTED).
     * Toute erreur réseau, timeout ou erreur de désérialisation est loguée
     * avant le déclenchement du fallback.
     */
    public Mono<PaymentResponse> analyzeTransaction(FraudRequest request) {
        return this.webClient.post()
                .uri("/predict")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(FraudResponse.class)
                .timeout(Duration.ofSeconds(3))                               // J2 — timeout réseau
                .doOnError(e -> logger.error(                                 // J3 — log avant failover
                        "ML service indisponible ou erreur de désérialisation — "
                        + "déclenchement du failover fail-closed (risk=0.99). "
                        + "Cause : {} — {}",
                        e.getClass().getSimpleName(), e.getMessage(), e))
                .onErrorReturn(new FraudResponse("Error", 0.99))
                .flatMap(fraudResponse -> Mono.fromCallable(() -> {           // J4 — I/O bloquant sur boundedElastic
                    double risk = fraudResponse.probabilite_fraude();
                    String status;
                    String message;

                    if (risk < 0.70) {
                        status = "APPROVED";
                        message = "Transaction autorisée.";
                    } else if (risk < 0.85) {
                        status = "MANUAL_REVIEW";
                        message = "Transaction suspecte : en attente de revue manuelle.";
                    } else {
                        status = "REJECTED";
                        message = "Transaction bloquée : Risque de fraude trop élevé.";
                    }

                    PaymentTransaction transaction = new PaymentTransaction(
                            request.features(),
                            risk,
                            status,
                            message
                    );
                    transactionRepository.save(transaction);
                    logger.info("Transaction persistée — status={} riskScore={}", status, risk);

                    return new PaymentResponse(status, risk, message);
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * Retourne les 50 dernières transactions sous forme de liste plate.
     * Le frontend attend un tableau JSON, pas un objet Page.
     */
    public Mono<List<PaymentTransaction>> getTransactionHistory() {
        return Mono.fromCallable(() -> transactionRepository
                        .findAll(PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt")))
                        .getContent())
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Supprime toutes les transactions de l'historique.
     * deleteAll() est bloquant (JPA) — exécuté sur boundedElastic.
     */
    public Mono<Void> clearHistory() {
        return Mono.<Void>fromRunnable(transactionRepository::deleteAll)
                .subscribeOn(Schedulers.boundedElastic());
    }
}
