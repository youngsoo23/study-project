package com.young.studyproject.document.infrastructure;

import com.young.studyproject.document.domain.Document;
import com.young.studyproject.document.domain.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DocumentRepositoryImpl implements DocumentRepository {

    private final DocumentJpaRepository documentJpaRepository;

    @Override
    public Document save(Document document) {
        return toDomain(documentJpaRepository.save(toEntity(document)));
    }

    @Override
    public boolean existsByPaymentId(Long paymentId) {
        return documentJpaRepository.existsByPaymentId(paymentId);
    }

    private DocumentJpaEntity toEntity(Document document) {
        return DocumentJpaEntity.builder()
                .id(document.getId())
                .paymentId(document.getPaymentId())
                .documentNumber(document.getDocumentNumber())
                .createdAt(document.getCreatedAt())
                .build();
    }

    private Document toDomain(DocumentJpaEntity entity) {
        return Document.builder()
                .id(entity.getId())
                .paymentId(entity.getPaymentId())
                .documentNumber(entity.getDocumentNumber())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
