# CLAUDE.md

이 문서는 Claude가 이 저장소에서 빠르고 일관되게 작업하기 위한 프로젝트 가이드다.

## 작업 및 탐색 범위

- 모든 코드 분석, 파일 검색, 문자열 검색은 현재 프로젝트 루트인 `study-project/` 내부에서만 수행한다.
- `..`을 이용해 상위 디렉터리로 이동하거나, 상위·형제 프로젝트·사용자 홈 디렉터리를 검색하지 않는다.
- `find`, `rg`, `grep` 등의 검색 명령은 항상 현재 프로젝트 루트를 작업 디렉터리로 지정하고 프로젝트 내부 경로만 대상으로 실행한다.
- 프로젝트 밖의 파일이나 정보가 꼭 필요하면 임의로 탐색하지 말고, 필요한 이유와 대상을 사용자에게 먼저 알리고 확인받는다.
- 빌드 도구가 사용하는 시스템 캐시나 설치된 JDK처럼 명령 실행 과정에서 자동 참조되는 외부 환경은 직접 분석하거나 수정하지 않는다.

## 프로젝트 개요

- Java/Spring 기반의 학습용 REST API 프로젝트다.
- 핵심 기능은 사용자(`user`)와 게시글(`post`) CRUD 및 동적 검색이다.
- `example` 패키지는 Java 문법과 기능을 직접 실행해 보는 학습 코드다.
- 기본 패키지는 `com.young.studyproject`다. Gradle의 `group` 값과 다르므로 새 Java 파일은 기존 패키지 구조를 따른다.

## 기술 스택

- Java 25 (Gradle toolchain)
- Spring Boot 4.1.0
- Spring Web MVC, Spring Data JPA, Jakarta Validation
- QueryDSL 5.1.0 (`jakarta`)
- H2 인메모리 DB (`MODE=MySQL`)
- Lombok, JUnit 5, Gradle Wrapper

로컬 환경에 Java 25가 없으면 빌드가 실패할 수 있다. 시스템 Gradle 대신 항상 Wrapper를 사용한다.

## 자주 쓰는 명령어

```bash
./gradlew clean build   # 생성 코드 재생성 + 전체 빌드/테스트
./gradlew test          # 테스트
./gradlew bootRun       # 애플리케이션 실행 (기본 8080)
./gradlew compileJava   # 빠른 컴파일 및 QueryDSL Q 클래스 생성
```

- H2 콘솔: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:studydb;MODE=MySQL`
- 현재 자동화 테스트는 Spring context 로드 테스트만 있으므로 기능 변경 시 관련 테스트를 추가한다.

## 디렉터리와 책임

```text
src/main/java/com/young/studyproject/
├── common/
│   ├── config/          # 공통 Spring 설정 (QueryDSL 등)
│   └── exception/       # 전역 예외 처리와 에러 응답
├── user/
│   ├── presentation/    # Controller, 요청/응답 DTO
│   ├── application/     # Service, Command/Result DTO
│   ├── domain/          # 순수 도메인 모델, Repository 포트
│   └── infrastructure/  # JPA 엔티티/Repository, QueryDSL, 포트 구현
├── post/                # user와 동일한 계층 구조
└── example/
    ├── functional/      # java.util.function 학습 API
    └── record/          # Java record 학습 API
```

추가 문서:

- `doc/functional-interface-example.md`: 함수형 인터페이스 예제 API와 호출법
- `study-doc/interview_debrief.md`: 예제 코드의 학습 배경
- `src/main/resources/application.yaml`: H2/JPA 런타임 설정

## 아키텍처 규칙

`user`와 `post` 기능을 수정하거나 새 도메인 기능을 추가할 때 다음 의존 방향을 유지한다.

```text
presentation -> application -> domain <- infrastructure
```

- Controller는 Request record를 application Command로 변환하고, Result를 Response record로 변환한다.
- Service는 유스케이스, 트랜잭션, 존재 여부 확인을 담당한다.
  - 클래스 기본값은 `@Transactional(readOnly = true)`다.
  - 생성/수정/삭제 메서드에만 `@Transactional`을 붙인다.
- domain 모델은 JPA 애노테이션이 없는 불변 객체다. 변경은 setter 대신 새 객체를 반환하는 메서드로 처리한다.
- domain의 `Repository` 인터페이스가 포트이며 application은 Spring Data/JPA 타입에 직접 의존하지 않는다.
- infrastructure는 별도의 `*JpaEntity`와 Spring Data `*JpaRepository`를 사용한다.
- `*RepositoryImpl`에서 도메인 객체와 JPA 엔티티를 명시적으로 상호 변환한다.
- 복합/동적 검색은 QueryDSL custom repository에 둔다. 선택 조건이 없으면 `BooleanExpression`에서 `null`을 반환해 조건을 생략한다.
- `post`는 DB 연관관계 객체 대신 `Long userId`를 저장한다. 기존 설계를 임의로 `@ManyToOne`으로 바꾸지 않는다.

## 구현 관례

- 생성자 주입은 Lombok `@RequiredArgsConstructor`를 사용한다.
- 요청/응답 및 계층 간 DTO는 `record`를 우선 사용한다.
- 요청 검증은 presentation DTO의 Jakarta Validation 애노테이션과 Controller의 `@Valid`로 처리한다.
- 리소스 미존재는 `EntityNotFoundException`, 잘못된 입력/중복은 현재 관례상 `IllegalArgumentException`을 사용한다.
- 전역 오류 응답은 `ErrorResponse(String message)` 형식이다. 새 예외를 도입하면 `GlobalExceptionHandler`도 함께 검토한다.
- 목록 변환은 Stream의 `.toList()`, DTO 변환은 `from(...)` 정적 팩터리 패턴을 따른다.
- 생성 시각과 수정 시각은 현재 Service/domain에서 `LocalDateTime.now()`로 설정한다.
- 기존 코드는 4칸 들여쓰기와 명시적인 import를 사용한다. 불필요한 전면 리팩터링이나 새 프레임워크 도입은 피한다.
- 학습용 `example` 코드는 의도를 설명하는 주석이 중요하다. 간결화 과정에서 학습 포인트를 제거하지 않는다.
- Controller를 새로 만들거나 엔드포인트를 추가·변경하면 직접 호출해 볼 수 있는 `.http` 파일도 함께 생성하거나 갱신한다.
- HTTP 요청 파일은 프로젝트 루트의 `http/` 디렉터리에 기능별로 둔다(예: `http/user.http`, `http/record-example.http`).
- `.http` 파일에는 필요한 선행 데이터 생성 요청과 정상 동작을 확인할 대표 요청을 포함한다. 요청 사이에는 `###` 구분자를 사용하고, 서버 주소는 `@host = http://localhost:8080` 변수로 선언해 재사용한다.

## QueryDSL 주의사항

- Q 클래스는 `build/generated/querydsl`에 생성되며 소스셋에 자동 포함된다.
- `build/` 아래 생성 파일을 직접 작성하거나 수정하거나 커밋하지 않는다.
- JPA 엔티티 필드 변경 후 QueryDSL 컴파일 오류가 나면 `./gradlew clean compileJava`로 재생성한다.
- custom 구현체 이름(`*QuerydslRepositoryImpl`)은 Spring Data가 인식하는 인터페이스/구현 명명 규칙을 유지한다.

## 주요 API

- `/api/users`: 사용자 생성, 조회, 수정, 삭제; `name` 쿼리로 검색
- `/api/posts`: 게시글 생성, 조회, 수정, 삭제; `keyword`, `userId` 쿼리로 검색
- `/api/examples/functional-interfaces`: 함수형 인터페이스 학습 API
- `/api/examples/record`: record 학습 API

사용자와 게시글 데이터는 H2 인메모리에 저장되며 애플리케이션 종료 시 사라진다. 게시글 생성 전 해당 `userId`의 사용자가 있어야 한다.

## 작업 절차

1. 변경할 기능과 같은 계층의 기존 `user` 또는 `post` 구현을 먼저 참고한다.
2. API 변경이면 Request/Response, Command/Result, Service, domain, repository까지 영향 범위를 확인한다.
3. Controller 또는 엔드포인트를 추가·변경했다면 `http/` 아래의 관련 `.http` 파일을 생성하거나 갱신해 바로 테스트할 수 있게 한다.
4. 엔티티나 검색 조건 변경이면 JPA 매핑, 변환 메서드, QueryDSL 구현을 함께 확인한다.
5. 동작 변경에 맞는 자동화 테스트를 추가하고 최소 `./gradlew test`, 가능하면 `./gradlew clean build`를 실행한다. `.http` 파일은 수동 확인용이며 자동화 테스트를 대체하지 않는다.
6. 관련 학습 API나 사용법이 달라지면 `doc/` 또는 `study-doc/` 문서도 갱신한다.

## 완료 전 체크리스트

- 계층 의존 방향과 도메인/JPA 엔티티 분리를 유지했는가?
- 쓰기 작업에 트랜잭션이 지정되었는가?
- 입력 검증과 예외 응답을 고려했는가?
- Controller 변경에 대응하는 실행 가능한 `.http` 요청 파일이 있는가?
- QueryDSL 생성 소스를 직접 수정하지 않았는가?
- 테스트 또는 빌드가 통과하는가?
- 변경과 무관한 파일을 수정하지 않았는가?
