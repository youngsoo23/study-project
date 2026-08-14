package com.young.studyproject.payment.presentation.dto;

import com.young.studyproject.payment.application.dto.PaymentResult;
import com.young.studyproject.payment.domain.PaymentStatus;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        String orderId,
        PaymentStatus status,
        boolean documentFailureForced,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static PaymentResponse from(PaymentResult result) {
        return new PaymentResponse(
                result.id(),
                result.orderId(),
                result.status(),
                result.documentFailureForced(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
