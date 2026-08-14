package com.young.studyproject.payment.application.dto;

import com.young.studyproject.payment.domain.Payment;
import com.young.studyproject.payment.domain.PaymentStatus;
import java.time.LocalDateTime;

public record PaymentResult(
        Long id,
        String orderId,
        PaymentStatus status,
        boolean documentFailureForced,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static PaymentResult from(Payment payment) {
        return new PaymentResult(
                payment.getId(),
                payment.getOrderId(),
                payment.getStatus(),
                payment.isDocumentFailureForced(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
