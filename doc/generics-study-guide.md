# 제네릭(Generics) 공부 가이드

`doc/async-example.md`의 `thenApply` 예제에서 나오는 `CompletableFuture<CompletableFuture<String>>` 같은 타입을
어떻게 읽고, 어떤 순서로 익힐지 정리한 문서다. 예제 API가 아니라 학습 방법에 대한 문서다.

이 문법의 이름은 **제네릭(Generics)** 이다. 중첩된 형태도 특별한 문법이 아니라
타입 파라미터 자리에 또 다른 제네릭 타입이 들어간 것뿐이다.

## 1. 타입 읽는 법: 바깥에서 안으로

```text
CompletableFuture< CompletableFuture< String > >
       상자               상자          알맹이
```

"String을 담은 상자를, 다시 담은 상자". 껍질을 벗기려면 `join()`을 두 번 해야 한다는 사실이 타입에 그대로 적혀 있다.
관련 코드는 `AsyncExampleService#runChain`.

익숙해지기 전까지는 안쪽부터 읽지 말고 **항상 가장 바깥 타입부터** 읽는다.
`Map<String, List<Function<Integer, String>>>`도 "Map이다 → 키는 String → 값은 List다 → 그 안은 Function이다" 순서로 끊어 읽으면 어렵지 않다.

## 2. 중첩 상자 문제는 `CompletableFuture`만의 얘기가 아니다

같은 구조가 모든 컨테이너 타입에서 반복된다. 이 패턴을 알아보는 것이 핵심 학습 포인트다.

| 컨테이너 | 값을 반환하는 함수 (`map` 계열) | 같은 컨테이너를 반환하는 함수 (`flatMap` 계열) |
|---|---|---|
| `Stream<T>` | `map` | `flatMap` |
| `Optional<T>` | `map` | `flatMap` |
| `CompletableFuture<T>` | `thenApply` | `thenCompose` |

이름만 다르고 구조는 같다. 한 문장으로 줄이면:

> 함수가 **값**을 반환하면 `map`, 함수가 **같은 종류의 상자**를 반환하면 `flatMap`.

`map` 자리에 상자를 반환하는 함수를 넣으면 항상 `Optional<Optional<T>>`, `Stream<Stream<T>>`,
`CompletableFuture<CompletableFuture<T>>` 같은 중첩이 생긴다.

## 3. 단계별 로드맵

### 1단계. 타입 파라미터를 직접 만들어 본다

남의 제네릭을 읽기 전에 내가 하나 정의해 보는 쪽이 훨씬 빠르다.

```java
public class Box<T> {

    private final T value;

    public Box(T value) {
        this.value = value;
    }

    // 값을 반환하는 함수 -> Box<R>
    public <R> Box<R> map(Function<T, R> f) {
        return new Box<>(f.apply(value));
    }

    // Box를 반환하는 함수 -> 중첩을 평탄화
    public <R> Box<R> flatMap(Function<T, Box<R>> f) {
        return f.apply(value);
    }
}
```

`map`에 `Box`를 반환하는 함수를 넣어 `Box<Box<String>>`을 직접 만들어 보면 2번 내용이 바로 이해된다.

### 2단계. 제네릭 메서드

위 코드의 `public <R> Box<R> map(...)`에서 반환 타입 앞의 `<R>`이 무엇인지 구분한다.

- 클래스의 타입 파라미터 `T`: 인스턴스를 만들 때 정해진다.
- 메서드의 타입 파라미터 `R`: 메서드를 호출할 때마다 따로 추론된다.

### 3단계. 무공변(invariance)과 와일드카드

가장 많이 걸려 넘어지는 지점이다. 먼저 이것부터 이해한다.

```java
List<String> names = new ArrayList<>();
List<Object> objects = names;  // 컴파일 에러
```

`String`이 `Object`의 하위 타입이어도 `List<String>`은 `List<Object>`의 하위 타입이 **아니다**(무공변).
허용하면 `objects.add(1)` 로 `List<String>`에 Integer를 넣을 수 있게 되기 때문이다.

이 제약을 느슨하게 푸는 장치가 와일드카드이고, 기준은 **PECS(Producer-Extends, Consumer-Super)** 다.

- `? extends T` — 값을 **꺼내 쓰기만** 할 때 (producer). 읽기는 되고 쓰기는 안 된다.
- `? super T` — 값을 **넣기만** 할 때 (consumer). 쓰기는 되고 읽으면 `Object`로만 나온다.

실제 JDK 시그니처가 이렇게 생긴 이유다.

```java
public <U> CompletableFuture<U> thenApply(Function<? super T, ? extends U> fn)
```

`fn`은 `T`를 소비하고(super) `U`를 생산한다(extends). 이걸 읽을 수 있으면 표준 라이브러리 문서가 대부분 읽힌다.

### 4단계. 타입 소거(type erasure)

컴파일이 끝나면 런타임에는 타입 인자 정보가 지워지고 `List<String>`도 그냥 `List`가 된다.
제네릭에서 "왜 이건 안 되지?" 하는 것들은 대부분 여기서 나온다.

| 안 되는 것 | 이유 / 우회 |
|---|---|
| `new T[10]` | 런타임에 `T`를 모름. `(T[]) new Object[10]` 로 우회 |
| `x instanceof List<String>` | 타입 인자가 지워짐. `List<?>` 까지만 가능 |
| `void f(List<String>)` / `void f(List<Integer>)` 오버로딩 | 소거 후 시그니처가 같아 충돌 |
| `class MyEx<T> extends Exception` | 제네릭 클래스는 `Throwable` 상속 불가 |
| `static T field;` | static은 인스턴스 타입 파라미터를 쓸 수 없음 |

런타임에 타입 정보가 꼭 필요하면 `Class<T>` 토큰을 넘기거나,
Jackson의 `TypeReference`, Spring의 `ParameterizedTypeReference`처럼 익명 클래스로 타입을 붙잡는 방식을 쓴다.

## 4. 실전 공부법

### ① 컴파일러를 선생으로 쓴다

일부러 틀린 타입을 대입해서 에러 메시지로 실제 타입을 확인한다.

```java
String x = fetchUserName(name, 10).thenApply(u -> fetchGreeting(u, 10));
```

```text
incompatible types: CompletableFuture<CompletableFuture<String>> cannot be converted to String
```

"이 표현식의 타입이 뭘까"를 **먼저 적어보고** 컴파일러에게 채점받는 방식으로 반복하는 게 가장 빠르다.

### ② JShell로 즉시 확인한다

파일을 만들고 빌드할 필요 없이 타입 실험만 반복할 수 있다.

```bash
jshell
```

```text
jshell> import java.util.concurrent.*;
jshell> CompletableFuture.supplyAsync(() -> "hi").thenApply(s -> CompletableFuture.completedFuture(s))
jshell> /vars      // 추론된 타입이 그대로 출력된다
```

### ③ 학습 중에는 `var`를 쓰지 않는다

타입을 손으로 적어야 머리에 남는다. `var`는 익숙해진 뒤에 쓴다.

### ④ 표준 라이브러리 시그니처를 직접 읽는다

`java.util.function` (8개 기본 인터페이스) → `Optional` → `Stream` → `CompletableFuture` 순서가 무난하다.
이 저장소에는 `example/functional`, `example/record`, `example/async` 예제가 이미 있으므로 그 코드의 시그니처부터 읽으면 된다.

### ⑤ IDE 기능을 쓴다

체이닝 중간에서 타입이 어떻게 바뀌는지 눈으로 추적하는 습관을 들인다.
IntelliJ 기준으로 표현식 선택 후 타입 확인, 호출부에서 파라미터 시그니처 보기(`Ctrl+P`)를 자주 쓴다.

## 5. 이 프로젝트에서 해볼 연습

`example/generic` 패키지를 추가해 3단계의 `Box<T>`를 직접 구현하고,
`map`과 `flatMap`의 차이를 API 응답으로 확인해 보는 것이 기존 예제 패턴과 잘 맞는다.

- 응답 record에 실제 타입 문자열을 담는 필드를 두면 `AsyncExampleService#runChain`의 `nestedTypeWithThenApply`와 같은 방식이 된다.
- Controller를 추가하면 `http/generic-example.http`도 함께 만든다(`CLAUDE.md` 작업 절차 참고).

## 6. 참고 자료

- **Oracle Java Tutorials — Generics trail**: 무료이고 순서가 잘 잡혀 있다. 특히 Wildcards 챕터.
- **이펙티브 자바 5장 (아이템 26~33)**: 제네릭만 8개 아이템. 실무 기준으로 필독.
- **Angelika Langer, Java Generics FAQ**: 분량은 많지만 "이건 왜 안 되지?" 대부분의 답이 있다.

추천 순서: 튜토리얼로 문법을 훑고 → `Box<T>`를 직접 구현하고 → 이펙티브 자바로 관례를 정리한다.
