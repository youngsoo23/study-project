package com.young.studyproject.example.record;

import com.young.studyproject.example.record.dto.EqualityResponse;
import com.young.studyproject.example.record.dto.Money;
import com.young.studyproject.example.record.dto.MoneyCalcResponse;
import com.young.studyproject.example.record.dto.UserCard;
import com.young.studyproject.example.record.dto.ValidationResponse;
import com.young.studyproject.user.domain.User;
import com.young.studyproject.user.domain.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * study-doc/interview_debrief.md 15번 항목(Record 클래스) 학습용 예제.
 * record가 자동으로 만들어주는 것과, compact constructor 같은 부가 기능을 실제로 확인해본다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecordExampleService {

    private final UserRepository userRepository;

    // 접근자는 getName()이 아니라 name()이다. toString()도 Money[currency=..., amount=...] 형태로 자동 생성된다.
    public List<UserCard> findAllUserCards() {
        return userRepository.findAll().stream()
                .map(UserCard::from)
                .toList();
    }

    // equals/hashCode가 모든 필드 기준으로 자동 생성되므로, 값이 같으면 다른 인스턴스여도 동등하고
    // HashSet/Map 키로 바로 쓸 수 있다.
    public EqualityResponse compareSameValueCards(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. id=" + userId));

        UserCard first = UserCard.from(user);
        UserCard second = UserCard.from(user);

        Set<UserCard> cards = new HashSet<>();
        cards.add(first);
        cards.add(second);

        return new EqualityResponse(first, second, first.equals(second), first == second, cards.size());
    }

    // compact constructor가 검증(음수 금액)과 정규화(krw -> KRW)를 모두 처리한다.
    public ValidationResponse createMoney(String currency, long amount) {
        String input = currency + " " + amount;
        try {
            Money money = new Money(currency, amount);
            return new ValidationResponse(input, true, "생성 성공: " + money);
        } catch (IllegalArgumentException e) {
            return new ValidationResponse(input, false, e.getMessage());
        }
    }

    // 불변 객체라 setter가 없고, 값 변경은 새 인스턴스를 반환하는 방식으로만 가능하다.
    public MoneyCalcResponse addMoney(long baseAmount, long addAmount) {
        Money original = Money.won(baseAmount);
        Money added = Money.won(addAmount);
        Money result = original.plus(added);

        return new MoneyCalcResponse(original, added, result, original.amount() == baseAmount);
    }

    // record는 클래스 상속은 못 하지만 인터페이스 구현은 가능하다. Money는 Comparable을 구현했다.
    public List<Money> sortAmounts(List<Long> amounts) {
        return amounts.stream()
                .map(Money::won)
                .sorted()
                .toList();
    }
}
