# Java & Spring 기초 다시 잡기 --- 1일차 학습 정리

> 7년차 백엔드 개발자 관점에서 단순 암기보다 **개념 → 코드 → Spring/실무
> 연결**을 목표로 정리한 학습 노트

------------------------------------------------------------------------

## 0. 전체 학습 흐름

어제는 Java 객체지향에서 시작해서 Spring의 핵심 원리까지 잠깐 연결한 뒤,
다시 Java 상속으로 돌아왔다.

``` text
Java 객체 / 클래스
    ↓
캡슐화
    ↓
인터페이스
    ↓
다형성
    ↓
Spring 연결
    ├─ DI
    ├─ IoC
    ├─ Bean
    ├─ @Component / @Bean
    ├─ AOP
    ├─ Proxy
    └─ @Transactional / self-invocation
    ↓
다시 Java
    ↓
상속
    ↓
오버라이딩 / 오버로딩
    ↓
추상 클래스

[다음 학습]
추상 클래스 vs 인터페이스
→ Collection
→ Generic
→ Lambda
→ 함수형 인터페이스
→ Stream
→ Exception
→ JVM
```

------------------------------------------------------------------------

# 1. 클래스와 객체

## 클래스

클래스는 **객체를 만들기 위한 설계도**라고 생각할 수 있다.

``` java
public class Member {
    String name;
    int age;
}
```

아직 실제 `Member` 객체가 만들어진 것은 아니다.

## 객체

`new`를 사용하면 실제 객체가 생성된다.

``` java
Member member = new Member();

member.name = "영수";
member.age = 40;
```

여기서:

``` java
Member member
```

`member`는 참조 변수이고,

``` java
new Member()
```

에서 실제 객체가 생성된다.

### 핵심

객체지향에서는 객체를 단순한 **데이터 저장통**으로만 생각하지 않는다.

객체는 자신의 **상태(state)** 와 그 상태를 다루는 **행동(behavior)** 을
함께 가질 수 있다.

------------------------------------------------------------------------

# 2. 캡슐화

다음과 같이 외부에서 상태를 직접 변경할 수 있다고 해보자.

``` java
public class Order {
    public OrderStatus status;
}
```

그러면 외부에서:

``` java
order.status = OrderStatus.CANCEL;
```

처럼 객체의 규칙과 상관없이 값을 변경할 수 있다.

그래서 상태를 숨긴다.

``` java
public class Order {
    private OrderStatus status;
}
```

하지만 단순히 setter를 제공한다고 캡슐화가 완성되는 것은 아니다.

``` java
public void setStatus(OrderStatus status) {
    this.status = status;
}
```

이렇게 하면 여전히 외부가 원하는 상태로 마음대로 변경할 수 있다.

## 의미 있는 행동을 제공한다

``` java
public class Order {

    private OrderStatus status;

    public void cancel() {
        if (status == OrderStatus.SHIPPING) {
            throw new IllegalStateException(
                "배송 중에는 취소할 수 없습니다."
            );
        }

        this.status = OrderStatus.CANCEL;
    }
}
```

외부에서는:

``` java
order.cancel();
```

만 호출한다.

### 핵심

> 캡슐화는 객체의 내부 상태와 상태를 변경하는 규칙을 객체 내부에 숨기고,
> 외부에는 필요한 행동만 제공하는 것이다.

따라서:

``` java
order.setStatus(CANCEL);
```

보다:

``` java
order.cancel();
```

이 객체의 의도를 더 잘 표현할 수 있다.

------------------------------------------------------------------------

# 3. 인터페이스

결제 방법이 여러 개 있다고 생각해보자.

-   카드
-   카카오페이
-   네이버페이

모두 공통적으로 **결제한다**는 행동을 가지고 있다.

이 공통된 계약을 인터페이스로 표현할 수 있다.

``` java
public interface Payment {
    void pay(int amount);
}
```

쉽게 읽으면:

> `Payment`라면 `pay()`를 제공해야 한다.

구현체는 이 계약을 구현한다.

``` java
public class CardPayment implements Payment {

    @Override
    public void pay(int amount) {
        System.out.println("카드 결제");
    }
}
```

``` java
public class KakaoPayment implements Payment {

    @Override
    public void pay(int amount) {
        System.out.println("카카오페이 결제");
    }
}
```

------------------------------------------------------------------------

# 4. 다형성

다음 코드가 가능하다.

``` java
Payment payment = new CardPayment();
```

또는:

``` java
Payment payment = new KakaoPayment();
```

그리고:

``` java
payment.pay(10000);
```

를 호출한다.

실제 객체가 `CardPayment`라면 카드 결제가 실행되고, `KakaoPayment`라면
카카오페이 결제가 실행된다.

### 핵심

> 같은 타입과 같은 메서드 호출이라도 실제 객체에 따라 서로 다르게 동작할
> 수 있다.

``` text
Payment payment
      │
      ├─ CardPayment  → CardPayment.pay()
      │
      └─ KakaoPayment → KakaoPayment.pay()
```

이것이 **다형성(Polymorphism)** 이다.

------------------------------------------------------------------------

# 5. 의존성과 DI

다음 코드는 `OrderService`가 구체적인 `CardPayment`에 직접 의존한다.

``` java
public class OrderService {

    private final CardPayment payment =
        new CardPayment();
}
```

그러면 카카오페이로 변경하려면 `OrderService`도 수정해야 한다.

인터페이스에 의존하도록 바꾸면:

``` java
public class OrderService {

    private final Payment payment;

    public OrderService(Payment payment) {
        this.payment = payment;
    }
}
```

`OrderService`는 구체적인 결제 방식이 무엇인지 알 필요가 없다.

외부에서:

``` java
new OrderService(new CardPayment());
```

또는:

``` java
new OrderService(new KakaoPayment());
```

처럼 넣어줄 수 있다.

이렇게 필요한 의존성을 외부에서 넣어주는 것을 **DI(Dependency Injection,
의존성 주입)** 라고 한다.

------------------------------------------------------------------------

# 6. Spring IoC와 Bean

일반 Java에서는 우리가 직접 객체를 생성한다.

``` java
CardPayment payment = new CardPayment();
OrderService service = new OrderService(payment);
```

Spring에서는 객체의 생성과 관리를 Spring이 담당할 수 있다.

``` java
@Component
public class CardPayment implements Payment {
}
```

``` java
@Service
public class OrderService {

    private final Payment payment;

    public OrderService(Payment payment) {
        this.payment = payment;
    }
}
```

Spring이 객체를 만들고 필요한 의존성을 연결해준다.

## Bean

> **Spring Container가 생성하고 관리하는 객체를 Bean이라고 한다.**

``` text
Spring Container

┌──────────────────────────┐
│ CardPayment Bean         │
│ OrderService Bean        │
│ MemberService Bean       │
└──────────────────────────┘
```

## IoC

**IoC(Inversion of Control, 제어의 역전)** 는 객체의 생성과 관리에 대한
제어권이 개발자의 코드에서 Spring으로 넘어가는 것을 의미한다.

``` text
직접 new하고 연결
      ↓
Spring이 생성하고 연결
```

### DI와 IoC 구분

-   **IoC**: 누가 객체의 생성과 관리를 제어하는가?
-   **DI**: 필요한 의존성을 어떻게 전달받는가?

DI는 IoC를 구현하는 대표적인 방법 중 하나다.

------------------------------------------------------------------------

# 7. @Component / @Service / @Repository

`@Component`가 붙은 클래스는 Component Scan을 통해 Spring Bean으로
등록될 수 있다.

``` java
@Component
public class CardPayment {
}
```

`@Service`, `@Repository`, `@Controller`도 Component 계열이다.

``` text
@Component
   ├─ @Service
   ├─ @Repository
   └─ @Controller
```

역할을 좀 더 명확하게 표현하기 위해 나누어 사용한다.

-   `@Service`: 비즈니스 로직
-   `@Repository`: 데이터 접근
-   `@Controller`: 웹 요청 처리

------------------------------------------------------------------------

# 8. @Configuration과 @Bean

클래스에 `@Component`를 직접 붙이지 않고 설정을 통해 Bean을 등록할 수도
있다.

``` java
@Configuration
public class PaymentConfig {

    @Bean
    public PaymentClient paymentClient() {
        return new PaymentClient();
    }
}
```

특히 외부 라이브러리 클래스처럼 직접 `@Component`를 붙일 수 없거나, 객체
생성 과정에 세밀한 설정이 필요한 경우 유용하다.

### 차이

  구분        `@Component`           `@Bean`
  ----------- ---------------------- ----------------------------
  적용 위치   클래스                 메서드
  등록 방식   Component Scan         설정 메서드
  생성 방식   Spring이 자동 생성     생성 방법을 직접 정의
  대표 사용   직접 만든 Service 등   외부 라이브러리, 설정 객체

------------------------------------------------------------------------

# 9. AOP

Service마다 실행 시간 측정, 로깅, 트랜잭션 같은 코드를 직접 넣으면
비즈니스 로직과 공통 기능이 섞인다.

``` java
public void order() {
    long start = System.currentTimeMillis();

    // 주문 처리

    long end = System.currentTimeMillis();
}
```

이런 공통 부가 기능을 비즈니스 로직에서 분리해서 적용하려는 것이
**AOP**의 핵심 아이디어다.

대표적인 공통 기능:

-   로깅
-   트랜잭션
-   권한 검사
-   실행 시간 측정

------------------------------------------------------------------------

# 10. Proxy

Spring AOP는 주로 Proxy를 이용한다.

Proxy는 쉽게 말하면 **대리 객체**다.

``` text
Controller
    ↓
  Proxy
    ↓
OrderService
```

Proxy가 실제 객체 호출 앞뒤에서 공통 기능을 처리할 수 있다.

개념적으로:

``` java
public void order() {

    // 공통 기능 Before

    target.order();

    // 공통 기능 After
}
```

실제 `OrderService`의 비즈니스 코드를 수정하지 않고 부가 기능을 적용할
수 있다.

------------------------------------------------------------------------

# 11. @Transactional과 Proxy

다음 코드가 있다고 하자.

``` java
@Service
public class PaymentService {

    @Transactional
    public void payment() {
        // DB 작업
    }
}
```

`@Transactional` 메서드를 호출한 뒤 Proxy가 만들어지는 것이 아니다.

Spring 애플리케이션이 시작될 때 AOP 대상 Bean에 대해 필요한 Proxy가
준비되고, 외부에서는 그 Proxy를 통해 호출하게 된다.

``` text
Spring 시작
    ↓
PaymentService 생성
    ↓
AOP / @Transactional 대상
    ↓
Proxy 준비
    ↓
외부에서 사용할 Bean 참조가 Proxy를 가리킴
```

Controller가:

``` java
paymentService.payment();
```

를 호출하면 개념적으로:

``` text
Controller
    ↓
PaymentService Proxy
    ↓
트랜잭션 시작
    ↓
실제 PaymentService.payment()
    ↓
성공 → COMMIT
예외 → ROLLBACK
```

이렇게 동작한다.

### 중요한 포인트

> Spring DI로 주입받은 객체가 항상 원본 객체 그 자체인 것은 아니다. AOP
> 적용 대상이라면 Proxy 객체가 주입될 수 있다.

------------------------------------------------------------------------

# 12. Self Invocation

다음 코드를 보자.

``` java
@Service
public class OrderService {

    public void order() {
        payment();
    }

    @Transactional
    public void payment() {
        // DB 작업
    }
}
```

외부에서 `order()`를 호출하면 처음에는 Proxy를 거칠 수 있다.

``` text
외부
 ↓
Proxy
 ↓
OrderService.order()
```

하지만 `order()` 내부에서:

``` java
payment();
```

를 호출하면 자기 자신의 메서드를 직접 호출한다.

``` text
Proxy
 ↓
OrderService.order()
       │
       └── payment()
```

`payment()` 호출이 다시 바깥의 Proxy를 통과하지 않는다.

따라서 일반적인 Spring Proxy 기반 AOP에서는 `payment()`에 선언한
`@Transactional`이 기대한 방식으로 적용되지 않을 수 있다.

### 면접 답변

> Spring AOP는 Proxy 기반으로 동작하기 때문에 외부 호출이 Proxy를 거쳐야
> 부가 기능이 적용됩니다. 같은 객체 내부에서 다른 메서드를 호출하는
> self-invocation의 경우 해당 호출이 Proxy를 다시 거치지 않기 때문에
> `@Transactional` 같은 AOP 기능이 적용되지 않는 문제가 발생할 수
> 있습니다.

------------------------------------------------------------------------

# 13. Java 상속

부모 클래스의 기능을 자식 클래스가 물려받을 수 있다.

``` java
public class Animal {

    public void eat() {
        System.out.println("먹는다");
    }
}
```

``` java
public class Dog extends Animal {

    public void bark() {
        System.out.println("멍멍");
    }
}
```

사용:

``` java
Dog dog = new Dog();

dog.eat();
dog.bark();
```

`Dog`는 `Animal`의 `eat()`을 사용할 수 있다.

------------------------------------------------------------------------

# 14. 오버라이딩

부모에게 물려받은 메서드를 자식이 다시 정의하는 것이다.

``` java
public class Animal {

    public void sound() {
        System.out.println("동물이 소리를 낸다");
    }
}
```

``` java
public class Dog extends Animal {

    @Override
    public void sound() {
        System.out.println("멍멍");
    }
}
```

그리고:

``` java
Animal animal = new Dog();

animal.sound();
```

결과:

``` text
멍멍
```

실제 객체가 `Dog`이기 때문에 오버라이딩된 `Dog.sound()`가 실행된다.

이 역시 다형성과 연결된다.

------------------------------------------------------------------------

# 15. 오버로딩

같은 이름의 메서드를 파라미터를 다르게 해서 여러 개 만드는 것이다.

``` java
public void pay(int amount) {
}

public void pay(int amount, String currency) {
}

public void pay(
    int amount,
    String currency,
    String cardNumber
) {
}
```

### 오버라이딩 vs 오버로딩

``` text
Overriding
→ 부모의 메서드를 자식이 재정의

Overloading
→ 같은 이름의 메서드를
  다른 파라미터로 여러 개 정의
```

------------------------------------------------------------------------

# 16. 추상 클래스

`abstract` 클래스는 직접 객체를 생성할 수 없다.

``` java
public abstract class Animal {

    public void eat() {
        System.out.println("먹는다");
    }
}
```

따라서:

``` java
new Animal();
```

은 불가능하다.

자식 클래스를 통해 사용한다.

``` java
public class Dog extends Animal {
}
```

## 추상 메서드

구현을 자식에게 맡길 수도 있다.

``` java
public abstract class Animal {

    public void eat() {
        System.out.println("먹는다");
    }

    public abstract void sound();
}
```

자식은 이를 구현한다.

``` java
public class Dog extends Animal {

    @Override
    public void sound() {
        System.out.println("멍멍");
    }
}
```

------------------------------------------------------------------------

# 17. 현재까지 가장 중요한 연결

각 개념을 따로 외우기보다 다음 흐름으로 이해한다.

``` text
객체
 ↓
객체가 상태와 행동을 가진다
 ↓
캡슐화
 ↓
객체 내부의 상태와 규칙을 보호한다
 ↓
인터페이스
 ↓
구현체들이 지켜야 할 계약
 ↓
다형성
 ↓
같은 타입으로 서로 다른 구현체를 다룬다
 ↓
DI
 ↓
구현체를 외부에서 넣어준다
 ↓
IoC
 ↓
Spring이 객체 생성/관리 제어권을 가진다
 ↓
Bean
 ↓
Spring Container가 객체를 관리한다
 ↓
AOP
 ↓
공통 부가 기능을 분리한다
 ↓
Proxy
 ↓
실제 객체 앞에서 호출을 가로챈다
 ↓
@Transactional
 ↓
Proxy가 트랜잭션 시작/commit/rollback 처리
```

------------------------------------------------------------------------

# 18. 다음 학습

다음에는 **추상 클래스와 인터페이스의 차이**부터 이어간다.

그 후 Java 기본기를 다음 순서로 진행한다.

``` text
추상 클래스 vs 인터페이스
        ↓
Collection
        ↓
Generic
        ↓
Lambda
        ↓
함수형 인터페이스
(Function / Predicate / Consumer / Supplier)
        ↓
Stream
        ↓
Exception
        ↓
JVM
```

## 다음 공부 전 스스로 답해볼 질문

1.  클래스와 객체의 차이는?
2.  `private`만 붙이면 캡슐화라고 할 수 있을까?
3.  인터페이스는 왜 사용할까?
4.  `Payment payment = new CardPayment()`가 가능한 이유는?
5.  다형성이란?
6.  DI와 IoC의 차이는?
7.  Spring Bean이란?
8.  `@Component`와 `@Bean`의 차이는?
9.  AOP와 Proxy는 어떤 관계인가?
10. `@Transactional`은 왜 Proxy와 관련 있는가?
11. self-invocation에서 `@Transactional`이 문제가 될 수 있는 이유는?
12. 오버라이딩과 오버로딩의 차이는?
13. 추상 클래스는 일반 클래스와 무엇이 다른가?

이 질문들에 자신의 말로 설명할 수 있다면 단순히 외운 것이 아니라 개념이
연결되기 시작한 것이다.
