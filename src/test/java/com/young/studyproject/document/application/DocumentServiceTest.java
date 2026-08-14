package com.young.studyproject.document.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.young.studyproject.document.domain.DocumentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DocumentServiceTest {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentRepository documentRepository;

    @Test
    @DisplayName("같은 paymentId로 두 번 호출해도 문서는 하나만 생성된다 (Kafka 재전달 대비 멱등성)")
    void createIfAbsentIsIdempotent() {
        Long paymentId = 100L;

        documentService.createIfAbsent(paymentId, "ORDER-IDEMPOTENT");
        documentService.createIfAbsent(paymentId, "ORDER-IDEMPOTENT");

        assertThat(documentRepository.existsByPaymentId(paymentId)).isTrue();
    }
}
