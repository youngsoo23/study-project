package com.young.studyproject.example.record.dto;

/**
 * record가 자동 생성해주는 것 외에 직접 추가할 수 있는 기능들을 한곳에 모아둔 예제.
 *
 * <p>컴파일러가 자동으로 만들어주는 것: 전 인자 생성자, 접근자(name()), equals, hashCode, toString.
 * 모든 필드는 private final이라 setter가 없는 완전한 불변 객체다.
 * record는 final이라 클래스 상속은 불가능하지만, 인터페이스 구현은 가능하다.
 */
public record Money(String currency, long amount) implements Comparable<Money> {

    /**
     * compact constructor: 파라미터 목록을 다시 선언하지 않고 검증/정규화만 작성한다.
     * 필드 할당(this.currency = currency)은 컴파일러가 이 블록 끝에 자동으로 붙여주므로,
     * 여기서 파라미터를 재할당하면 그 값이 그대로 필드에 들어간다.
     */
    public Money {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("통화는 필수입니다.");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("금액은 음수일 수 없습니다. amount=" + amount);
        }
        currency = currency.toUpperCase();
    }

    // record 안에도 static 팩토리 메서드를 둘 수 있다.
    public static Money won(long amount) {
        return new Money("KRW", amount);
    }

    // 불변 객체이므로 값을 바꾸는 대신 새 인스턴스를 반환한다.
    // 접근자가 getAmount()가 아니라 amount()인 점에 주의.
    public Money plus(Money other) {
        if (!currency.equals(other.currency())) {
            throw new IllegalArgumentException("통화가 다릅니다. " + currency + " vs " + other.currency());
        }
        return new Money(currency, amount + other.amount());
    }

    @Override
    public int compareTo(Money other) {
        return Long.compare(amount, other.amount());
    }
}
