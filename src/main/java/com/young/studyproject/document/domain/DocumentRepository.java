package com.young.studyproject.document.domain;

/**
 * 도메인과 인프라스트럭쳐(JPA) 사이의 포트.
 */
public interface DocumentRepository {

    Document save(Document document);

    boolean existsByPaymentId(Long paymentId);
}
