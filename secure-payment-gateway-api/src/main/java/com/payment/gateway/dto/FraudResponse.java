package com.payment.gateway.dto;

public record FraudResponse(String prediction, double probabilite_fraude) {
}
