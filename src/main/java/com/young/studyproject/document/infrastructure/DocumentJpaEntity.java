package com.young.studyproject.document.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Kafka 메시지는 중복 전달될 수 있으므로 UNIQUE 제약으로도 중복 문서 생성을 막는다.
    @Column(nullable = false, unique = true)
    private Long paymentId;

    @Column(nullable = false)
    private String documentNumber;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public DocumentJpaEntity(Long id, Long paymentId, String documentNumber, LocalDateTime createdAt) {
        this.id = id;
        this.paymentId = paymentId;
        this.documentNumber = documentNumber;
        this.createdAt = createdAt;
    }
}
