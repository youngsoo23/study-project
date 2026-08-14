package com.young.studyproject.payment.infrastructure;

import com.young.studyproject.payment.domain.Payment;
import com.young.studyproject.payment.domain.PaymentRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    public Payment save(Payment payment) {
        return toDomain(paymentJpaRepository.save(toEntity(payment)));
    }

    @Override
    public Optional<Payment> findById(Long id) {
        return paymentJpaRepository.findById(id).map(this::toDomain);
    }

    private PaymentJpaEntity toEntity(Payment payment) {
        return PaymentJpaEntity.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .status(payment.getStatus())
                .documentFailureForced(payment.isDocumentFailureForced())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    private Payment toDomain(PaymentJpaEntity entity) {
        return Payment.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .status(entity.getStatus())
                .documentFailureForced(entity.isDocumentFailureForced())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
