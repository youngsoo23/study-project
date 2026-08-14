package com.young.studyproject.outbox.domain;

import java.util.List;

/**
 * 도메인과 인프라스트럭쳐(JPA) 사이의 포트.
 */
public interface OutboxRepository {

    OutboxEvent save(OutboxEvent event);

    List<OutboxEvent> findPending(int limit);
}
