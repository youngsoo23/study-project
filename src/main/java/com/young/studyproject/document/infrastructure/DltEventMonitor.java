package com.young.studyproject.document.infrastructure;

import com.young.studyproject.payment.application.dto.PaymentCompletedEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/**
 * 최근 DLT로 들어온 이벤트를 메모리에 잠깐 보관해두는 용도의 학습용 컴포넌트.
 * 콘솔 로그만으로는 "정말 DLT에 도착했는지"를 코드(테스트)로 확인하기 어려워서 둔다.
 * 영속 저장소가 아니므로 애플리케이션 재시작 시 사라진다.
 */
@Component
public class DltEventMonitor {

    private static final int MAX_SIZE = 20;

    private final List<PaymentCompletedEvent> recentEvents = new CopyOnWriteArrayList<>();

    public void record(PaymentCompletedEvent event) {
        recentEvents.add(event);
        while (recentEvents.size() > MAX_SIZE) {
            recentEvents.removeFirst();
        }
    }

    public List<PaymentCompletedEvent> recentEvents() {
        return List.copyOf(recentEvents);
    }
}
