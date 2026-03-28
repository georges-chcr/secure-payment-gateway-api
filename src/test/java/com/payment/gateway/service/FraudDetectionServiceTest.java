package com.payment.gateway.service;

import com.payment.gateway.dto.FraudRequest;
import com.payment.gateway.model.PaymentTransaction;
import com.payment.gateway.repository.TransactionRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests de résilience pour FraudDetectionService.
 *
 * Utilise MockWebServer pour simuler le service ML Python sans démarrer
 * de contexte Spring, ce qui rend les tests rapides et déterministes.
 *
 * Scénarios couverts :
 *   1. Nominal       — ML répond 200, probabilite_fraude=0.05 → APPROVED
 *   2. Erreur 500    — onErrorReturn s'active           → REJECTED (risk=0.99)
 *   3. Timeout réseau — délai > 3 s                    → REJECTED (risk=0.99)
 */
@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    private MockWebServer mockWebServer;
    private FraudDetectionService service;

    @Mock
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Le repository.save() retourne l'entité reçue (comportement JPA standard)
        when(transactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Instanciation directe — pas de contexte Spring nécessaire grâce à
        // l'injection par constructeur ; on passe l'URL du MockWebServer.
        service = new FraudDetectionService(
                WebClient.builder(),
                mockWebServer.url("/").toString(),
                transactionRepository
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Construit une requête avec 30 features valides (V1..V28 + Time + Amount). */
    private FraudRequest buildTestRequest() {
        List<Double> features = List.of(
                -1.36, 0.96,  1.19,  0.26,  0.09,  0.46,  0.21,  0.17,
                 0.11, 0.14, -0.29,  0.41, -1.24,  0.03,  0.01,  0.01,
                -0.42, 0.30,  0.51,  0.17,  0.26, -0.05,  0.05, -0.09,
                 0.15, 0.04,  0.32, -0.13,  0.24,  2.69
        );
        return new FraudRequest(features);
    }

    // ── Test 1 : Cas nominal ──────────────────────────────────────────────────

    @Test
    void analyzeTransaction_nominal_shouldReturnApproved() {
        // Given — ML engine répond 200 avec une probabilité de fraude basse
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"prediction\":\"Normal\",\"probabilite_fraude\":0.05}"));

        // When / Then
        StepVerifier.create(service.analyzeTransaction(buildTestRequest()))
                .assertNext(response -> {
                    assertThat(response.status()).isEqualTo("APPROVED");
                    assertThat(response.riskScore()).isEqualTo(0.05);
                    assertThat(response.message()).contains("autorisée");
                })
                .verifyComplete();
    }

    // ── Test 2 : Failover sur erreur serveur (500) ────────────────────────────

    @Test
    void analyzeTransaction_mlServiceReturns500_shouldTriggerFailoverAndReject() {
        // Given — ML engine renvoie une erreur 500
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        // When / Then — onErrorReturn(FraudResponse("Error", 0.99)) doit s'activer
        StepVerifier.create(service.analyzeTransaction(buildTestRequest()))
                .assertNext(response -> {
                    assertThat(response.status()).isEqualTo("REJECTED");
                    assertThat(response.riskScore()).isEqualTo(0.99);
                    assertThat(response.message()).contains("bloquée");
                })
                .verifyComplete();
    }

    // ── Test 3 : Failover sur timeout réseau ─────────────────────────────────

    @Test
    void analyzeTransaction_mlServiceTimeout_shouldTriggerFailoverAndReject() {
        // Given — ML engine répond après 5 s ; le timeout WebClient est à 3 s
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"prediction\":\"Normal\",\"probabilite_fraude\":0.05}")
                .setBodyDelay(5, TimeUnit.SECONDS));

        // When / Then — timeout(3s) déclenche TimeoutException → onErrorReturn
        StepVerifier.create(service.analyzeTransaction(buildTestRequest()))
                .assertNext(response -> {
                    assertThat(response.status()).isEqualTo("REJECTED");
                    assertThat(response.riskScore()).isEqualTo(0.99);
                })
                .verifyComplete();
    }
}
