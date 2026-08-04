package com.young.studyproject.example.record;

import com.young.studyproject.example.record.dto.EqualityResponse;
import com.young.studyproject.example.record.dto.Money;
import com.young.studyproject.example.record.dto.MoneyCalcResponse;
import com.young.studyproject.example.record.dto.UserCard;
import com.young.studyproject.example.record.dto.ValidationResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 각 엔드포인트는 record의 기능 하나씩을 직접 확인해본다.
 */
@RestController
@RequestMapping("/api/examples/record")
@RequiredArgsConstructor
public class RecordExampleController {

    private final RecordExampleService exampleService;

    @GetMapping("/user-cards") // 접근자(name()) + toString 자동 생성
    public List<UserCard> userCards() {
        return exampleService.findAllUserCards();
    }

    @GetMapping("/equality") // equals/hashCode 자동 생성
    public EqualityResponse equality(@RequestParam Long userId) {
        return exampleService.compareSameValueCards(userId);
    }

    @GetMapping("/compact-constructor") // compact constructor의 검증 + 정규화
    public ValidationResponse compactConstructor(
            @RequestParam(defaultValue = "krw") String currency,
            @RequestParam long amount
    ) {
        return exampleService.createMoney(currency, amount);
    }

    @GetMapping("/immutable") // setter 없이 새 인스턴스를 반환하는 불변 스타일
    public MoneyCalcResponse immutable(
            @RequestParam long baseAmount,
            @RequestParam long addAmount
    ) {
        return exampleService.addMoney(baseAmount, addAmount);
    }

    @GetMapping("/sorted") // 인터페이스 구현(Comparable) 가능
    public List<Money> sorted(@RequestParam List<Long> amounts) {
        return exampleService.sortAmounts(amounts);
    }
}
