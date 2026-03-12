package com.payment.gateway.dto;

import java.util.List;

public record FraudRequest(List<Double> features) {
}
