package com.payment.gateway.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "transactions")
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime timestamp;

    @ElementCollection
    private List<Double> features;

    private double riskScore;
    private String status;
    private String message;


    public PaymentTransaction() {}


    public PaymentTransaction(List<Double> features, double riskScore, String status, String message) {
        this.timestamp = LocalDateTime.now();
        this.features = features;
        this.riskScore = riskScore;
        this.status = status;
        this.message = message;
    }

    // Getters
    public Long getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public List<Double> getFeatures() { return features; }
    public double getRiskScore() { return riskScore; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
}
