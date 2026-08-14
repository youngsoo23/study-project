package com.young.studyproject.payment.domain;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Payment {

    private final Long id;
    private final String orderId;
    private final PaymentStatus status;
    private final boolean documentFailureForced;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public Payment complete() {
        return Payment.builder()
                .id(this.id)
                .orderId(this.orderId)
                .status(PaymentStatus.COMPLETE)
                .documentFailureForced(this.documentFailureForced)
                .createdAt(this.createdAt)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public Payment forceDocumentFailure() {
        return Payment.builder()
                .id(this.id)
                .orderId(this.orderId)
                .status(this.status)
                .documentFailureForced(true)
                .createdAt(this.createdAt)
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
