package com.young.studyproject.payment.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, Long> {
}
