package com.payment.gateway.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record FraudRequest(
        @NotNull(message = "La liste de features ne peut pas être nulle.")
        @Size(
                min = 30,
                max = 30,
                message = "La liste de features doit contenir exactement 30 éléments (V1-V28 + Time + Amount)."
        )
        List<@NotNull Double> features
) {}
