package com.young.studyproject.document.application;

import com.young.studyproject.document.domain.Document;
import com.young.studyproject.document.domain.DocumentRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentService {

    private final DocumentRepository documentRepository;

    /**
     * Kafka는 동일 메시지가 다시 전달될 수 있으므로(재시도, 컨슈머 재시작 등) Consumer는 멱등성을 고려해야 한다.
     * paymentId로 이미 문서가 있는지 먼저 확인하고, DB의 UNIQUE 제약(paymentId)으로 한 번 더 방어한다.
     */
    @Transactional
    public void createIfAbsent(Long paymentId, String orderId) {
        if (documentRepository.existsByPaymentId(paymentId)) {
            log.info("[DOCUMENT] 이미 생성된 문서라 건너뜀 paymentId={}", paymentId);
            return;
        }

        Document document = Document.builder()
                .paymentId(paymentId)
                .documentNumber("DOC-" + UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .build();

        documentRepository.save(document);
        log.info("[DOCUMENT] 문서 생성 성공 paymentId={} orderId={}", paymentId, orderId);
    }
}
