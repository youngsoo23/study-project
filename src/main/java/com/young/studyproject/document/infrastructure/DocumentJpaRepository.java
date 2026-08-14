package com.young.studyproject.document.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentJpaRepository extends JpaRepository<DocumentJpaEntity, Long> {

    boolean existsByPaymentId(Long paymentId);
}
