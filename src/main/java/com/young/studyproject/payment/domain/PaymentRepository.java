package com.young.studyproject.payment.domain;

import java.util.Optional;

/**
 * 도메인과 인프라스트럭쳐(JPA) 사이의 포트.
 */
public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(Long id);
}
