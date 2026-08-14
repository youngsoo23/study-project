package com.young.studyproject.document.domain;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Document {

    private final Long id;
    private final Long paymentId;
    private final String documentNumber;
    private final LocalDateTime createdAt;
}
