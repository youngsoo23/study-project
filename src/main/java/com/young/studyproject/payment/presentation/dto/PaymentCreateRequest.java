package com.young.studyproject.payment.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentCreateRequest(@NotBlank String orderId) {
}
