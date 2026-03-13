package com.payment.gateway.dto;

public record PaymentResponse(String status, double riskScore, String message) {
}
