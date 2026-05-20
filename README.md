# 🥃 BE_ONZ (Onz Cocktail Backend)

> ONZ 칵테일 서비스의 **백엔드 서버**입니다.  
칵테일을 **태그 기반으로 관리**하고, **관리자 페이지에서 칵테일/태그를 등록**하며,  
앱/프론트에서 **소셜 로그인 → 토큰 발급 → 칵테일 목록/검색** 흐름으로 사용하는 구조입니다.

- Framework: **Spring Boot 3.4.1**
- Language: **Java 17**
- Build: **Gradle**
- DB: **MariaDB** (설정값 환경변수로 주입)
- 인증: **JWT + 소셜로그인(Naver, Kakao, Google, Apple) "전략 패턴"**
- 파일: **칵테일 이미지 -> AWS S3 업로드**
- 뷰: **Thymeleaf 기반 Admin 페이지**
- Query: **QueryDSL 5 (jakarta)** 로 태그/필터 검색

---

## 1. 아키텍처 설명

```text
Client (Mobile/Web)
   │
   ├─ REST API (/api/...) ← 기본 api 주소
   ↓
Spring Boot (BE_ONZ)
   ├─ Controller (Auth, Member, Cocktail)
   ├─ Service
   │    ├─ Auth/OAuth2Service  ← 소셜 로그인, 토큰 발급
   │    ├─ CocktailService     ← 태그/도수/맛/시즌으로 필터링
   │    └─ AdminService        ← 관리자 태그/칵테일 등록
   ├─ Repository (JPA + QueryDSL)
   ├─ Security (JWTFilter, SecurityConfig)
   └─ S3Service (이미지 업로드)
      ↓
   MariaDB
   AWS S3
```

---

## 2. 필요한 기술스택 설명

| 구분 | 사용기술 | 비고                                                     |
|------|----------|--------------------------------------------------------|
| 언어 | Java 17 | Gradle toolchain 설정됨                                   |
| 프레임워크 | Spring Boot 3.4.1 | Spring Web,Spring Security, JPA, Validation, Thymeleaf |
| DB | MariaDB (또는 MySQL 호환) | JDBC url 사용                                            |
| ORM | Spring Data JPA + QueryDSL 5 | 태그 기반 검색에 사용                                           |
| 인증 | Spring Security + JWT + OAuth2 Client | 로그인 지원 : Kakao/Naver/Google/Apple                      |
| 캐시 | Spring Cache + Caffeine | 토큰/로그인 처리 최적화용                                         |
| 파일 | spring-cloud-starter-aws 2.2.6.RELEASE | S3 업로드                                                 |
| 뷰 | Thymeleaf | admin 페이지                                              |
| 빌드 | Gradle (wrapper 포함) | `./gradlew` 로 빌드                                       |

---

## 3. 빌드 방법

### 환경 변수 준비

>자세한 사항은 application.properties 확인필요
```properties
spring.jwt.secret=${JWT_SECRET}
spring.datasource.url=jdbc:mariadb://${DB_HOST}/cocktail
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
aws.s3.accesskey=${AWS_S3_ACCESS_KEY}
aws.s3.secretKey=${AWS_S3_SECRET_KEY}
spring.security.oauth2.client.registration.kakao.client-id=${KAKAO_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.apple.client-id=${APPLE_CLIENT_ID}
```

### 빌드 및 실행

```bash
./gradlew clean build
java -jar build/libs/application-0.0.1-SNAPSHOT.jar
```


## 4. GitHub Action 적용

`.github/workflows`에 CI/CD 정의가 포함되어 있습니다.

```yaml
name: BE_ONZ CI/CD
on:
  push:
    branches: [ main ]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - run: ./gradlew clean build
      - run: docker build -t be-onz .
```

---

## 5. 필요한 API Key 목록

| 이름 | 용도 |
|------|------|
| `JWT_SECRET` | access/refresh 토큰 서명 |
| `DB_HOST`, `DB_USER`, `DB_PASSWORD` | MariaDB 연결 |
| `AWS_S3_ACCESS_KEY`, `AWS_S3_SECRET_KEY`, `S3_BUCKET` | AWS S3 업로드 |
| `KAKAO_CLIENT_ID`, `KAKAO_SECRET` | 카카오 로그인 |
| `NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET` | 네이버 로그인 |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | 구글 로그인 |
| `APPLE_CLIENT_ID`, `APPLE_KEY_ID`, `APPLE_TEAM_ID`, `APPLE_PRIVATE_KEY` | 애플 로그인 |

---

## 6. 리팩토링이 필요한 부분

| 영역 | 설명 | 개선 제안                                                |
|------|------|------------------------------------------------------|
| Auth 로직 | 토큰/로그인 중복 | AuthFacade 도입 (로그인 중복 검사 분리)                         |
| 예외 처리 | Controller마다 다름 | 전역 ExceptionHandler 통합                               |
| Cocktail 검색 | if문 많음 | QueryDSL PredicateBuilder 분리 또는 Elestic Search 사용 고려 |
| Admin/REST 혼합 | 운영 분리 어려움 | admin 모듈 분리 고려                                       |
| 테스트 | 단위테스트 부족 | MockWebServer / Mockito 보강                           |
| S3Service | URL 하드코딩 | 프로퍼티화 필요                                             |

---

## 7. 주요 API

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/auth/social/login` | 소셜 로그인 |
| POST | `/api/auth/signup` | 회원가입 |
| POST | `/api/auth/logout` | 로그아웃 |
| GET | `/api/cocktail/list` | 칵테일 목록 |
| POST | `/api/cocktail/tags` | 태그별 검색 |
| POST | `/admin/manage/tag` | 태그 관리 |
| POST | `/admin/cocktail` | 칵테일 등록 |

---

## 8. 향후 개선 방향

- Swagger(OpenAPI) 문서 자동화
- Redis 캐시 도입 (카페인 Cache 대신)
- QueryDSL Predicate Builder화 (Or Elestic Search 도입)
- CloudFront 기반 이미지 CDN
- 통합 테스트 커버리지 확대

---

## 📚 문서

### 배포 및 운영 - HTTPS 설정

**[🚀 HTTPS 설정 방법 선택 가이드](./docs/HTTPS_빠른_시작_가이드.md)** ← **먼저 읽어보세요!**
- 3가지 방식 비교 및 선택 가이드
- 상황별 추천 방식
- 빠른 시작 명령어

#### 방식별 상세 가이드

- **[방식 A: 호스트 Nginx + Let's Encrypt](./docs/HTTPS_방식A_호스트_NGINX.md)**
  - ⏱️ 설정 시간: 5분 (가장 빠름)
  - 💰 비용: 무료
  - ✅ 추천: 빠르게 HTTPS만 적용하고 싶은 경우

- **[방식 B: Docker Compose Nginx + Certbot](./docs/HTTPS_방식B_도커_NGINX.md)**
  - ⏱️ 설정 시간: 15분
  - 💰 비용: 무료
  - ✅ 추천: 전체 인프라를 Docker로 통합하고 싶은 경우

- **[방식 C: AWS ALB + ACM](./docs/AWS_ALB_HTTPS_설정_가이드.md)** 🔥 **프로덕션 추천**
  - ⏱️ 설정 시간: 10분
  - 💰 비용: ~$16/월
  - ✅ 추천: 실제 서비스 운영, 확장성 필요

### 개발 가이드

- **[CLAUDE.md](./CLAUDE.md)**
  - 프로젝트 구조 및 아키텍처
  - 개발 환경 설정
  - 주요 패턴 및 컨벤션

---

# 응답 구조 (ResponseDto)
<pre>
{ code : 1 (성공) | -1 (실패), msg : {}, data : {} }
</pre>


# Exception
<pre>
  - Exception Handler 구현
  - AOP 구현 ( Validation 체크용 -> PostMapping | PutMapping )
</pre>



