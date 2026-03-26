# 로깅 시스템 가이드

ONZ Cocktail Backend의 로깅 수집 및 모니터링 시스템 사용 가이드입니다.

## 목차
- [접근 경로](#접근-경로)
- [로그 확인 방법](#로그-확인-방법)
- [에러 디버깅 방법](#에러-디버깅-방법)
- [Request ID 추적](#request-id-추적)
- [운영 환경 설정](#운영-환경-설정)

---

## 접근 경로

> **중요**: Context Path가 `/onz`로 설정되어 있어 모든 경로에 `/onz`를 붙여야 합니다.

### 개발 환경 (localhost)

| 이름 | URL | 설명 |
|------|-----|------|
| **실시간 로그** | http://localhost:8080/onz/actuator/logfile | 텍스트 형식 로그 스트리밍 ✅ |
| **Actuator** | http://localhost:8080/onz/actuator | 모든 모니터링 엔드포인트 목록 |
| **Health Check** | http://localhost:8080/onz/actuator/health | 애플리케이션 상태 확인 |
| **Metrics** | http://localhost:8080/onz/actuator/metrics | 성능 메트릭 확인 |
| **Loggers** | http://localhost:8080/onz/actuator/loggers | 로그 레벨 확인/변경 |
| **로컬 파일** | `./logs/Onz.log` | 직접 파일 확인 (tail -f) |

### 운영 환경 (Production)

| 이름 | URL | 설명 |
|------|-----|------|
| **실시간 로그** | http://onz-cocktail.kr/onz/actuator/logfile | 텍스트 형식 로그 스트리밍 ✅ |
| **Actuator** | http://onz-cocktail.kr/onz/actuator | 모든 모니터링 엔드포인트 목록 |
| **로그 파일** | SSH → `logs/Onz.log` | EC2 서버 직접 접속 |

> **참고**: Spring Boot Admin UI는 context-path 호환 문제로 비활성화되었습니다. Actuator API와 로그 파일로 모든 기능을 사용할 수 있습니다.

---

## 로그 확인 방법

### 1. Actuator Logfile 엔드포인트 (추천)

**접속**: http://localhost:8080/onz/actuator/logfile

**특징**:
- 텍스트 형식으로 로그 전체 내용 출력
- curl이나 스크립트로 자동화 가능
- 로그 파일 다운로드 가능

**사용 예시**:
```bash
# 전체 로그 보기
curl http://localhost:8080/onz/actuator/logfile

# 최근 100줄만 보기
curl http://localhost:8080/onz/actuator/logfile | tail -100

# 특정 Request ID로 필터링
curl http://localhost:8080/onz/actuator/logfile | grep "a1b2c3d4"

# 에러 로그만 보기
curl http://localhost:8080/onz/actuator/logfile | grep -i "error\|exception"

# 로그 파일 다운로드
curl http://localhost:8080/onz/actuator/logfile -o backend.log
```

### 2. 로컬 파일 직접 확인 (가장 빠름)

**경로**: `./logs/Onz.log`

**사용 방법**:
```bash
# 실시간 로그 모니터링
tail -f logs/Onz.log

# 최근 100줄 보기
tail -100 logs/Onz.log

# 특정 키워드 검색
grep "ERROR" logs/Onz.log

# Request ID로 추적
grep "a1b2c3d4" logs/Onz.log
```

**로그 보관 정책**:
- 일별 롤링: `Onz-yyyy-MM-dd.log` 형식으로 자동 분리
- 보관 기간: 최근 7일
- 최대 용량: 1GB

---

## 에러 디버깅 방법

### 프론트엔드 개발자를 위한 가이드

#### 1. 에러 발생 시 Response에서 Request ID 확인

모든 API 응답 헤더에 `X-Request-ID`가 포함되어 있습니다.

**예시 Response Headers**:
```
X-Request-ID: a1b2c3d4
Content-Type: application/json
```

**React/JavaScript에서 확인**:
```javascript
fetch('/onz/api/v2/cocktails/detail?cocktailId=1')
  .then(response => {
    const requestId = response.headers.get('X-Request-ID');
    console.log('Request ID:', requestId);
    return response.json();
  })
  .catch(error => {
    // 에러 발생 시 Request ID를 백엔드 개발자에게 전달
    console.error('Error with Request ID:', requestId);
  });
```

#### 2. Request ID로 로그 추적

백엔드 개발자에게 Request ID를 전달하면, 해당 요청의 모든 로그를 추적할 수 있습니다.

**Admin UI에서 검색**:
1. http://localhost:8080/onz/log-viewer 접속
2. Logfile 메뉴 클릭
3. Request ID (`a1b2c3d4`) 검색

**로그 예시**:
```
2026-02-03 22:08:15 [http-nio-8080-exec-1] [a1b2c3d4] INFO  c.a.common.filter.RequestIdFilter - 요청 시작 - Method: GET, URI: /onz/api/v2/cocktails/detail, IP: 127.0.0.1
2026-02-03 22:08:15 [http-nio-8080-exec-1] [a1b2c3d4] ERROR c.a.domain.cocktail.service.CocktailService - 칵테일 조회 실패: Cocktail not found with id: 999
2026-02-03 22:08:15 [http-nio-8080-exec-1] [a1b2c3d4] WARN  c.a.common.exception.CustomExceptionHandler - Resource Exception : 칵테일을 찾을 수 없음
2026-02-03 22:08:15 [http-nio-8080-exec-1] [a1b2c3d4] INFO  c.a.common.filter.RequestIdFilter - 요청 완료 - Status: 404
```

#### 3. 에러 응답 형식

모든 에러는 표준화된 `ResponseDto` 형식으로 반환됩니다.

**성공 응답**:
```json
{
  "code": 1,
  "msg": "칵테일 조회 성공 (v2)",
  "data": { ... }
}
```

**에러 응답**:
```json
{
  "code": -1,
  "msg": "리소스를 찾을 수 없음",
  "data": "Cocktail not found with id: 999"
}
```

#### 4. 에러 유형별 HTTP Status Code

| HTTP Status | code | 설명 | 예시 |
|-------------|------|------|------|
| 200 OK | 1 | 성공 | 정상 조회 |
| 400 Bad Request | -1 | 잘못된 요청 | 유효성 검증 실패 |
| 401 Unauthorized | -1 | 인증 실패 | JWT 토큰 만료 |
| 404 Not Found | -1 | 리소스 없음 | 존재하지 않는 칵테일 ID |
| 500 Internal Server Error | -1 | 서버 에러 | 예상치 못한 오류 |

---

## Request ID 추적

### Request ID란?

각 HTTP 요청마다 부여되는 고유 식별자 (UUID 8자리)로, 로그 추적 및 디버깅에 사용됩니다.

### Request ID 생성 및 전달 흐름

1. **클라이언트 요청** → 헤더에 `X-Request-ID` 없음
2. **RequestIdFilter** → UUID 생성 (`a1b2c3d4`)
3. **MDC에 저장** → 모든 로그에 자동 포함
4. **응답 헤더에 추가** → 클라이언트가 확인 가능
5. **로그 파일 기록** → Request ID와 함께 저장

### 로그 포맷

```
{날짜} {시간} [{스레드}] [{Request ID}] {로그레벨} {클래스명} - {메시지}
```

**예시**:
```
2026-02-03 22:08:15 [http-nio-8080-exec-1] [a1b2c3d4] INFO c.a.common.filter.RequestIdFilter - 요청 시작 - Method: GET, URI: /onz/api/v2/cocktails/detail, IP: 127.0.0.1
```

### Request ID로 전체 요청 흐름 추적

```bash
# Request ID로 해당 요청의 모든 로그 추출
grep "a1b2c3d4" logs/Onz.log

# 결과 예시:
# 2026-02-03 22:08:15 [http-nio-8080-exec-1] [a1b2c3d4] INFO  ... - 요청 시작 - Method: GET, URI: /onz/api/v2/cocktails/detail
# 2026-02-03 22:08:15 [http-nio-8080-exec-1] [a1b2c3d4] INFO  ... - Credential ID: kakao_123456789
# 2026-02-03 22:08:15 [http-nio-8080-exec-1] [a1b2c3d4] INFO  ... - 요청 완료 - Status: 200
```

---

## 운영 환경 설정

### EC2 배포 체크리스트

http://onz-cocktail.kr/onz/log-viewer 로 접근하려면 다음을 확인하세요:

#### ✅ 1. 코드 설정 완료
- [x] `application-docker.yaml`에 Spring Boot Admin 설정 추가됨
- [x] `docker-compose.yml`에 로그 볼륨 마운트 설정됨 (`./logs:/app/logs`)

#### ✅ 2. 도메인 DNS 설정
- [ ] `onz-cocktail.kr` 도메인이 EC2 퍼블릭 IP로 A 레코드 연결되어 있는지 확인
- [ ] DNS 전파 완료 확인: `nslookup onz-cocktail.kr`

#### ✅ 3. EC2 보안 그룹 설정
- [ ] 인바운드 규칙에 **80번 포트** 허용 (0.0.0.0/0)
- [ ] 선택사항: 443번 포트 (HTTPS)

#### ✅ 4. Docker 배포
```bash
# EC2 서버에서 실행
cd /path/to/Cocktail_backend

# 환경 변수 설정 (.env 파일 생성)
# POSTGRES_USER, POSTGRES_PASSWORD, JWT_SECRET 등

# Docker Compose로 실행
docker-compose up -d --build

# 로그 확인
docker logs -f cocktail-api
```

#### ✅ 5. 접근 확인
```bash
# 1. Health Check
curl http://onz-cocktail.kr/onz/actuator/health

# 2. Admin UI (브라우저에서)
http://onz-cocktail.kr/onz/log-viewer

# 3. 로그 파일
curl http://onz-cocktail.kr/onz/actuator/logfile
```

#### ✅ 6. 로그 디렉토리 권한 설정
```bash
# EC2 서버에서 logs 디렉토리 생성 및 권한 설정
mkdir -p logs
chmod 755 logs
```

### 보안 주의사항

현재 Actuator와 Admin UI가 **인증 없이 노출**되어 있습니다.

**운영 환경 배포 전 필수 조치**:

#### 1. Actuator 보안 설정

`application-prod.yml`에 추가:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info  # 필요한 엔드포인트만 노출
  endpoint:
    health:
      show-details: when-authorized  # 인증된 사용자만 상세 정보 확인
```

#### 2. SecurityConfig에 인증 추가

`src/main/java/com/application/common/auth/config/SecurityConfig.java`:
```java
// 현재 (위험)
.requestMatchers(ACTUATOR_URLS).permitAll()

// 변경 필요 (안전)
.requestMatchers("/actuator/health", "/actuator/info").permitAll()  // Health만 공개
.requestMatchers("/actuator/**", "/log-viewer/**").hasRole("ADMIN")  // 관리자만 접근
```

#### 3. 외부 모니터링 도구 사용 (선택)

프로덕션 환경에서는 다음 도구 사용 권장:
- **ELK Stack** (Elasticsearch + Logstash + Kibana): 로그 수집 및 분석
- **Grafana + Prometheus**: 메트릭 시각화
- **Sentry**: 에러 트래킹
- **AWS CloudWatch**: AWS 환경에서 통합 모니터링

---

## 로그 레벨 변경 (런타임)

### 개발 중 특정 패키지의 로그 레벨 변경

**방법 1: Admin UI 사용**
1. http://localhost:8080/onz/log-viewer 접속
2. Loggers 메뉴 클릭
3. 패키지 검색 (예: `com.application.domain.cocktail`)
4. 로그 레벨 변경 (DEBUG, INFO, WARN, ERROR)

**방법 2: Actuator API 사용**
```bash
# 현재 로그 레벨 확인
curl http://localhost:8080/onz/actuator/loggers/com.application.domain.cocktail

# 로그 레벨 변경 (DEBUG로)
curl -X POST http://localhost:8080/onz/actuator/loggers/com.application.domain.cocktail \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'
```

---

## FAQ

### Q1. 로그가 보이지 않아요
- `./logs/Onz.log` 파일이 존재하는지 확인
- 애플리케이션이 실행 중인지 확인
- `log.path` 설정이 올바른지 확인 (application.properties)

### Q2. Admin UI가 404 에러가 나요
- Context Path를 포함한 경로로 접속: `/onz/log-viewer`
- 애플리케이션이 정상 구동되었는지 확인: `/onz/actuator/health`

### Q3. Request ID를 찾을 수 없어요
- 응답 헤더에서 `X-Request-ID` 확인
- RequestIdFilter가 정상 동작하는지 확인
- 로그에 `[NO_REQUEST_ID]`가 표시되면 필터 문제

### Q4. 로그 파일이 너무 커져요
- 현재 설정: 7일간 보관, 최대 1GB
- `logback-spring.xml`에서 `<maxHistory>`와 `<totalSizeCap>` 조정

### Q5. 운영 환경에서 민감한 정보가 노출될까요?
- 현재 Actuator가 인증 없이 노출되어 있어 위험
- 운영 배포 전 SecurityConfig 수정 필수
- 환경 변수, 데이터베이스 연결 정보 등이 노출될 수 있음

---

## 관련 파일

| 파일 | 설명 |
|------|------|
| `src/main/resources/logback-spring.xml` | Logback 로그 설정 (파일 경로, 롤링 정책) |
| `src/main/java/com/application/common/filter/RequestIdFilter.java` | Request ID 생성 및 MDC 저장 |
| `src/main/java/com/application/common/exception/CustomExceptionHandler.java` | 전역 예외 핸들러 및 에러 로깅 |
| `src/main/resources/application.yml` | Spring Boot Admin & Actuator 설정 |
| `src/main/java/com/application/common/auth/config/SecurityConfig.java` | Actuator 보안 설정 |
| `./logs/Onz.log` | 로그 파일 (실시간 기록) |

---

**작성일**: 2026-02-03
**담당자**: Backend Team