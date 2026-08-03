# 예제: 스트림 함수형 인터페이스 (`java.util.function`)

`study-doc/interview_debrief.md`의 **2번 "스트림 안에서 쓸 수 있는 함수형 인터페이스"** 항목을 실제 User/Post 데이터로 연습하기 위한 예제.

## 구성

- `com.young.studyproject.example.functional.FunctionalInterfaceExampleService`
  - `UserRepository`, `PostRepository` 포트를 그대로 사용해 실제 도메인 데이터를 다룸
- `com.young.studyproject.example.functional.FunctionalInterfaceExampleController`
  - 각 메서드를 API로 직접 호출해볼 수 있게 노출
- `com.young.studyproject.example.functional.dto`
  - 응답용 record 4종 (`UserSummaryResponse`, `PostSummaryResponse`, `CombineResponse`, `NormalizeNameResponse`)

## 엔드포인트 ↔ 함수형 인터페이스

| 엔드포인트 | 인터페이스 | 설명 |
|---|---|---|
| `GET /api/examples/functional-interfaces/user-names` | `Function<User, String>` | `stream().map()`의 인자 타입. T를 받아 R로 변환 |
| `GET /api/examples/functional-interfaces/users/search?nameKeyword=&emailDomain=` | `Predicate<User>` | `stream().filter()`의 인자 타입. `and()`로 두 조건을 조합 |
| `GET /api/examples/functional-interfaces/users/summaries` | `Consumer<User>` | `forEach()`의 인자 타입. 값을 반환하지 않고 부수 효과만 수행 |
| `GET /api/examples/functional-interfaces/users/first-or-guest` | `Supplier<User>` | `Optional.orElseGet()`처럼 인자 없이 값을 지연 생성 |
| `GET /api/examples/functional-interfaces/combine?userId=&postId=` | `BiFunction<User, Post, String>` | 두 인자를 받아 하나의 결과로 변환 |
| `GET /api/examples/functional-interfaces/normalize-name?name=` | `UnaryOperator<String>` | 입출력 타입이 같은 `Function`의 특수화 |
| `GET /api/examples/functional-interfaces/posts/latest?postId1=&postId2=` | `BinaryOperator<Post>` | 같은 타입 둘을 받아 같은 타입을 반환. `Comparator.comparing(...)`과 결합한 `maxBy`로 최신 글 선택 |

## 실행 및 테스트

```bash
./gradlew bootRun
```

```bash
# 데이터 준비
curl -X POST http://localhost:8080/api/users -H "Content-Type: application/json" \
  -d '{"name":"홍길동","email":"hong@example.com"}'
curl -X POST http://localhost:8080/api/posts -H "Content-Type: application/json" \
  -d '{"title":"첫 글","content":"내용1","userId":1}'

# 예제 호출
curl http://localhost:8080/api/examples/functional-interfaces/user-names
curl -G http://localhost:8080/api/examples/functional-interfaces/users/search \
  --data-urlencode "nameKeyword=길동" --data-urlencode "emailDomain=example.com"
curl http://localhost:8080/api/examples/functional-interfaces/users/summaries
curl http://localhost:8080/api/examples/functional-interfaces/users/first-or-guest
curl -G http://localhost:8080/api/examples/functional-interfaces/combine \
  --data-urlencode "userId=1" --data-urlencode "postId=1"
curl -G http://localhost:8080/api/examples/functional-interfaces/normalize-name \
  --data-urlencode "name=  young  "
curl -G http://localhost:8080/api/examples/functional-interfaces/posts/latest \
  --data-urlencode "postId1=1" --data-urlencode "postId2=2"
```

## 참고

원본 학습 항목: `study-doc/interview_debrief.md` 2번.
