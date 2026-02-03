# 로깅 시스템 최종 상태

## ✅ 구현 완료된 기능

### 1. Request ID 추적
- 모든 HTTP 요청에 고유 ID 부여 (UUID 8자리)
- 응답 헤더 `X-Request-ID`로 프론트엔드에 전달
- 로그에서 `[REQUEST_ID]` 형식으로 추적 가능

### 2. 로그 파일 저장
- **경로**: `./logs/Onz.log`
- **롤링**: 날짜별 자동 분리 (`Onz-2026-02-03.log`)
- **보관**: 최근 7일
- **최대 용량**: 1GB

### 3. Actuator API
- **실시간 로그**: `/onz/actuator/logfile`
- **Health Check**: `/onz/actuator/health`
- **Metrics**: `/onz/actuator/metrics`
- **Loggers**: `/onz/actuator/loggers`

### 4. 예외 처리 및 로깅
- 전역 예외 핸들러로 모든 에러 캡처
- Request ID와 함께 로그 기록
- 표준 ResponseDto 형식 반환

## 🚀 사용 방법

### 개발 환경

```bash
# 로컬 파일 모니터링 (추천)
tail -f logs/Onz.log

# API로 로그 확인
curl http://localhost:8080/onz/actuator/logfile | tail -100

# Request ID로 필터링
curl http://localhost:8080/onz/actuator/logfile | grep "a1b2c3d4"

# 에러만 보기
curl http://localhost:8080/onz/actuator/logfile | grep -i "error"
```

### 운영 환경

```bash
# EC2 SSH 접속 후
tail -f logs/Onz.log

# 또는 API 사용
curl http://onz-cocktail.kr/onz/actuator/logfile | tail -100
```

### 프론트엔드 에러 디버깅

1. **Response에서 Request ID 확인**
```javascript
fetch('/onz/api/v2/cocktails/detail?cocktailId=999')
  .then(response => {
    const requestId = response.headers.get('X-Request-ID');
    console.log('Request ID:', requestId);  // 예: "a1b2c3d4"
  });
```

2. **백엔드 개발자에게 Request ID 전달**
   - "칵테일 조회 시 500 에러 발생, Request ID: a1b2c3d4"

3. **백엔드에서 로그 검색**
```bash
curl http://localhost:8080/onz/actuator/logfile | grep "a1b2c3d4"

# 결과:
# 2026-02-03 22:53:37 [http-nio-8080-exec-2] [a1b2c3d4] INFO  ... - 요청 시작 - Method: GET, URI: /onz/api/v2/cocktails/detail
# 2026-02-03 22:53:37 [http-nio-8080-exec-2] [a1b2c3d4] ERROR ... - 칵테일 조회 실패: Cocktail not found
# 2026-02-03 22:53:37 [http-nio-8080-exec-2] [a1b2c3d4] INFO  ... - 요청 완료 - Status: 404
```

## 🔧 주요 설정 파일

| 파일 | 설명 |
|------|------|
| `src/main/resources/logback-spring.xml` | Logback 설정 (롤링, 포맷) |
| `src/main/java/.../filter/RequestIdFilter.java` | Request ID 생성 및 MDC 저장 |
| `src/main/java/.../exception/CustomExceptionHandler.java` | 전역 예외 처리 및 로깅 |
| `src/main/resources/application.yml` | Actuator 설정 |
| `./logs/Onz.log` | 현재 로그 파일 |
| `./logs/Onz-yyyy-MM-dd.log` | 과거 로그 (날짜별) |

## ❌ 비활성화된 기능

- **Spring Boot Admin UI**: context-path 호환 문제로 비활성화
  - 대안: Actuator API 또는 로컬 파일 사용

## 📊 로그 포맷

```
{날짜} {시간} [{스레드}] [{Request ID}] {레벨} {클래스} - {메시지}
```

**예시**:
```
2026-02-03 22:53:37 [http-nio-8080-exec-2] [a1b2c3d4] INFO  c.a.common.filter.RequestIdFilter - 요청 시작 - Method: GET, URI: /onz/api/v2/cocktails/detail, IP: 127.0.0.1
```

## 🔐 운영 환경 보안

현재 Actuator가 인증 없이 노출되어 있습니다. 운영 배포 전 다음 조치 필요:

```java
// SecurityConfig.java 수정 필요
.requestMatchers("/actuator/health", "/actuator/info").permitAll()
.requestMatchers("/actuator/**").hasRole("ADMIN")  // 추가
```

## 📝 추가 개선 사항 (선택)

나중에 필요 시 고려:
- **ELK Stack**: Elasticsearch + Logstash + Kibana
- **Grafana + Loki**: 전문 로그 분석 시스템
- **Sentry**: 실시간 에러 트래킹
- **AWS CloudWatch**: AWS 환경 통합 모니터링

---

**최종 업데이트**: 2026-02-03
**상태**: ✅ 정상 작동 중
