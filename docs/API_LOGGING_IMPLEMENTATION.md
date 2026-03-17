# API 로깅 구현 계획

AOP를 활용하여 모든 API 요청에 대해 Controller → Service 흐름과 응답 시간을 로그로 출력하는 기능 구현

## 목표

- 어느 API로 요청이 들어왔는지 (HTTP 메서드 + URI)
- 어떤 Controller 메서드가 호출되었는지
- 어떤 Service 메서드들이 호출되었는지
- 각 레이어별 실행 시간 및 전체 API 응답 시간

## 예상 로그 출력 형식

```
[GET /api/v2/cocktails] CocktailController.getCocktails() 시작
  → CocktailService.findAllWithFilter() 호출
  → CocktailService.findAllWithFilter() 완료 (45ms)
  → BookmarkService.getBookmarkedIds() 호출
  → BookmarkService.getBookmarkedIds() 완료 (12ms)
[GET /api/v2/cocktails] CocktailController.getCocktails() 완료 - 총 234ms
```

---

## 구현 단계

### 1단계: RequestContext DTO 생성

**파일:** `src/main/java/com/application/common/logging/RequestContext.java`

**목적:** ThreadLocal에 저장할 요청 컨텍스트 데이터 모델

**필드:**
```java
public class RequestContext {
    private String httpMethod;      // GET, POST, PUT, DELETE 등
    private String uri;             // /api/v2/cocktails
    private String controllerName;  // CocktailController
    private String controllerMethod; // getCocktails
    private long startTime;         // System.currentTimeMillis()

    // Constructor, Getter, Setter, Builder
}
```

---

### 2단계: ControllerLoggingAspect 구현

**파일:** `src/main/java/com/application/common/logging/ControllerLoggingAspect.java`

**어노테이션:**
- `@Aspect`
- `@Component`
- `@Slf4j`

**ThreadLocal 선언:**
```java
private static final ThreadLocal<RequestContext> REQUEST_CONTEXT = new ThreadLocal<>();

// Getter 메서드 (ServiceLoggingAspect에서 사용)
public static RequestContext getRequestContext() {
    return REQUEST_CONTEXT.get();
}
```

**포인트컷:**
```java
@Around("@within(org.springframework.web.bind.annotation.RestController)")
```

**구현 로직:**

**1) preHandle (메서드 실행 전):**
```java
// HttpServletRequest 가져오기
ServletRequestAttributes attributes =
    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
HttpServletRequest request = attributes.getRequest();

// HTTP 정보 추출
String httpMethod = request.getMethod();  // GET, POST 등
String uri = request.getRequestURI();     // /api/v2/cocktails

// Controller 정보 추출 (JoinPoint 사용)
String className = joinPoint.getTarget().getClass().getSimpleName();
String methodName = joinPoint.getSignature().getName();

// 시작 시간 기록
long startTime = System.currentTimeMillis();

// RequestContext 생성 및 ThreadLocal 저장
RequestContext context = RequestContext.builder()
    .httpMethod(httpMethod)
    .uri(uri)
    .controllerName(className)
    .controllerMethod(methodName)
    .startTime(startTime)
    .build();
REQUEST_CONTEXT.set(context);

// 로그 출력
log.info("[{} {}] {}.{}() 시작", httpMethod, uri, className, methodName);
```

**2) 메서드 실행:**
```java
Object result = joinPoint.proceed();
```

**3) afterHandle (메서드 실행 후):**
```java
long endTime = System.currentTimeMillis();
long executionTime = endTime - context.getStartTime();

log.info("[{} {}] {}.{}() 완료 - 총 {}ms",
    context.getHttpMethod(),
    context.getUri(),
    context.getControllerName(),
    context.getControllerMethod(),
    executionTime);
```

**4) finally 블록 (필수!):**
```java
finally {
    // ThreadLocal 메모리 누수 방지
    REQUEST_CONTEXT.remove();
}
```

**전체 구조:**
```java
@Around("...")
public Object logPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
    try {
        // preHandle
        Object result = joinPoint.proceed();
        // afterHandle
        return result;
    } finally {
        REQUEST_CONTEXT.remove();  // 반드시 정리!
    }
}
```

---

### 3단계: ServiceLoggingAspect 구현

**파일:** `src/main/java/com/application/common/logging/ServiceLoggingAspect.java`

**어노테이션:**
- `@Aspect`
- `@Component`
- `@Slf4j`

**포인트컷:**
```java
@Around("@within(org.springframework.stereotype.Service)")
```

**구현 로직:**

**1) preHandle (메서드 실행 전):**
```java
// ControllerLoggingAspect의 ThreadLocal에서 컨텍스트 가져오기
RequestContext context = ControllerLoggingAspect.getRequestContext();

// Service 정보 추출
String className = joinPoint.getTarget().getClass().getSimpleName();
String methodName = joinPoint.getSignature().getName();

// 시작 시간 기록
long startTime = System.currentTimeMillis();

// 로그 출력 (들여쓰기로 계층 표시)
if (context != null) {
    log.info("  → {}.{}() 호출", className, methodName);
} else {
    // Controller 없이 Service만 호출된 경우
    log.info("[Direct] {}.{}() 호출", className, methodName);
}
```

**2) 메서드 실행:**
```java
Object result = joinPoint.proceed();
```

**3) afterHandle (메서드 실행 후):**
```java
long executionTime = System.currentTimeMillis() - startTime;

if (context != null) {
    log.info("  → {}.{}() 완료 ({}ms)", className, methodName, executionTime);
} else {
    log.info("[Direct] {}.{}() 완료 ({}ms)", className, methodName, executionTime);
}
```

**전체 구조:**
```java
@Around("...")
public Object logPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
    RequestContext context = ControllerLoggingAspect.getRequestContext();
    String className = joinPoint.getTarget().getClass().getSimpleName();
    String methodName = joinPoint.getSignature().getName();
    long startTime = System.currentTimeMillis();

    // preHandle 로그

    try {
        Object result = joinPoint.proceed();
        // afterHandle 로그
        return result;
    } catch (Exception e) {
        // 에러 발생 시에도 로그
        long executionTime = System.currentTimeMillis() - startTime;
        log.error("  → {}.{}() 실패 ({}ms) - {}",
            className, methodName, executionTime, e.getMessage());
        throw e;
    }
}
```

---

### 4단계: 로그 레벨 설정

**파일:** `src/main/resources/application.properties`

**추가 내용:**
```properties
# Performance Logging
logging.level.com.application.common.logging=INFO
```

**개발 환경에서만 활성화하려면:**
- `application-dev.properties` 파일에만 추가
- 또는 로그 레벨을 DEBUG로 설정하고 운영 환경은 INFO로 유지

**선택적 설정 (더 상세한 로깅):**
```properties
# SQL 쿼리 로깅
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# HTTP 요청/응답 로깅
logging.level.org.springframework.web=DEBUG
```

---

### 5단계: 테스트 및 검증

**1) 애플리케이션 실행:**
```bash
./gradlew clean bootRun
```

**2) Swagger UI 접속:**
```
http://localhost:8080/swagger-ui.html
```

**3) 테스트할 API 목록:**
- `GET /api/v2/cocktails` - 칵테일 목록 조회
- `POST /api/v2/cocktails/{id}/bookmark` - 북마크 추가
- `GET /api/v2/cocktails/{id}` - 칵테일 상세 조회
- `GET /api/v2/members/profile` - 회원 프로필 조회

**4) 콘솔 로그 확인:**

**예상 출력 (칵테일 목록 조회):**
```
[GET /api/v2/cocktails] CocktailControllerV2.getCocktailListV2() 시작
  → CocktailService.getCocktailListV2() 호출
  → CocktailService.getCocktailListV2() 완료 (87ms)
  → BookmarkService.getBookmarkedCocktailIdsByMemberId() 호출
  → BookmarkService.getBookmarkedCocktailIdsByMemberId() 완료 (15ms)
[GET /api/v2/cocktails] CocktailControllerV2.getCocktailListV2() 완료 - 총 156ms
```

**예상 출력 (북마크 추가):**
```
[POST /api/v2/cocktails/123/bookmark] BookmarkController.addBookmark() 시작
  → BookmarkService.addBookmark() 호출
  → BookmarkService.addBookmark() 완료 (23ms)
[POST /api/v2/cocktails/123/bookmark] BookmarkController.addBookmark() 완료 - 총 45ms
```

**5) 검증 체크리스트:**
- [ ] HTTP 메서드(GET, POST 등)가 정확히 표시되는가?
- [ ] URI가 올바르게 출력되는가?
- [ ] Controller 클래스명과 메서드명이 표시되는가?
- [ ] Service 메서드 호출이 들여쓰기로 구분되는가?
- [ ] 각 Service 메서드의 실행 시간이 표시되는가?
- [ ] 전체 API 응답 시간이 정확히 측정되는가?
- [ ] 에러 발생 시에도 로그가 출력되는가?

---

## 주의사항

### 1. ThreadLocal 메모리 누수 방지
- **반드시 finally 블록에서 `ThreadLocal.remove()` 호출**
- Tomcat 같은 WAS는 쓰레드 풀을 재사용하므로, 정리하지 않으면 이전 요청의 데이터가 남음

### 2. 비동기 메서드 처리
- `@Async` 메서드는 별도 쓰레드에서 실행되므로 ThreadLocal 공유 안됨
- 필요시 `TaskDecorator`로 ThreadLocal 전파 필요

### 3. 성능 영향
- 로깅은 I/O 작업이므로 성능에 영향을 줄 수 있음
- 운영 환경에서는 로그 레벨을 조정하거나 샘플링 고려

### 4. 예외 처리
- Service에서 예외 발생 시에도 실행 시간 로그 출력
- `@ControllerAdvice`의 글로벌 예외 핸들러와 중복 로그 주의

### 5. AOP 순서
- 여러 Aspect가 있을 경우 `@Order` 어노테이션으로 실행 순서 제어
- 현재 프로젝트에는 `RequestBodyValidatorAspect`가 있으므로 순서 고려

---

## 추가 개선 아이디어

### 1. 느린 API 강조
```java
if (executionTime > 1000) {
    log.warn("[SLOW API] {} - {}ms", uri, executionTime);
}
```

### 2. 요청 파라미터 로깅
```java
Object[] args = joinPoint.getArgs();
log.info("Parameters: {}", Arrays.toString(args));
```

### 3. 응답 데이터 로깅 (선택적)
```java
log.debug("Response: {}", result);
```

### 4. MDC (Mapped Diagnostic Context) 활용
```java
MDC.put("requestId", UUID.randomUUID().toString());
// 로그에 requestId가 자동으로 포함됨
```

### 5. 메트릭 수집
- Micrometer 연동하여 Prometheus로 메트릭 전송
- API별 평균 응답 시간, 호출 횟수 등 수집

---

## 파일 생성 위치 요약

```
src/main/java/com/application/common/logging/
├── RequestContext.java              # 1단계
├── ControllerLoggingAspect.java     # 2단계
└── ServiceLoggingAspect.java        # 3단계

src/main/resources/
└── application.properties            # 4단계 (수정)
```

---

## 구현 순서

1. RequestContext DTO 생성
2. ControllerLoggingAspect 구현 및 테스트
3. ServiceLoggingAspect 구현 및 테스트
4. application.properties 로그 레벨 설정
5. 실제 API 호출하여 통합 테스트

각 단계마다 테스트하면서 진행하면 안전하게 구현 가능합니다!
