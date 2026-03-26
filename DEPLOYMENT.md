# EC2 배포 가이드

ONZ Cocktail Backend를 EC2에 배포하고 로그 모니터링 시스템을 사용하는 방법입니다.

## 배포 전 체크리스트

### 1. 도메인 설정 확인
- [ ] `onz-cocktail.kr` 도메인이 EC2 퍼블릭 IP로 A 레코드 연결됨
- [ ] DNS 전파 확인: `nslookup onz-cocktail.kr`

### 2. EC2 보안 그룹 설정
- [ ] 인바운드 규칙에 **80번 포트** 허용 (HTTP)
- [ ] 선택사항: **443번 포트** 허용 (HTTPS)
- [ ] **5432번 포트**: 같은 VPC 내에서만 허용 (PostgreSQL)

### 3. EC2 환경 설정
```bash
# Docker 및 Docker Compose 설치 확인
docker --version
docker-compose --version

# Git 설치 확인
git --version
```

---

## 배포 과정

### Step 1: 코드 가져오기
```bash
# EC2 서버에 SSH 접속
ssh -i your-key.pem ubuntu@your-ec2-ip

# 프로젝트 클론 (또는 git pull)
git clone https://github.com/MobileOnz/Cocktail_backend.git
cd Cocktail_backend

# 또는 기존 코드 업데이트
git pull origin dev
```

### Step 2: 환경 변수 설정
EC2 서버에 `.env` 파일 생성:

```bash
# .env 파일 생성
nano .env
```

`.env` 파일 내용:
```env
# PostgreSQL
POSTGRES_USER=cocktail_user
POSTGRES_PASSWORD=your_strong_password
POSTGRES_DB=cocktail

# JWT
JWT_SECRET=your-super-secret-key-for-production-at-least-32-chars-long

# OAuth2 - Kakao
KAKAO_ID=your_kakao_client_id
KAKAO_SECRET=your_kakao_client_secret
KAKAO_REDIRECT_URI=http://onz-cocktail.kr/login/oauth2/code/kakao

# OAuth2 - Naver
NAVER_ID=your_naver_client_id
NAVER_SECRET=your_naver_client_secret
NAVER_REDIRECT_URI=http://onz-cocktail.kr/login/oauth2/code/naver

# OAuth2 - Google
GOOGLE_ID=your_google_client_id
GOOGLE_SECRET=your_google_client_secret
GOOGLE_REDIRECT_URI=http://onz-cocktail.kr/login/oauth2/code/google

# OAuth2 - Apple
APPLE_SERVICE_ID=your_apple_service_id
APPLE_CLIENT_ID=your_apple_client_id
APPLE_REDIRECT_URI=http://onz-cocktail.kr/login/oauth2/code/apple
APPLE_TEAM_ID=your_apple_team_id
APPLE_KEY_ID=your_apple_key_id
APPLE_PRIVATE_KEY=your_apple_private_key
APPLE_AUDIENCE=https://appleid.apple.com
APPLE_PUBLIC_KEY_URL=https://appleid.apple.com/auth/keys
APPLE_TOKEN_URL=https://appleid.apple.com/auth/token
```

### Step 3: 로그 디렉토리 생성
```bash
# 로그 디렉토리 생성 및 권한 설정
mkdir -p logs
chmod 755 logs
```

### Step 4: Docker Compose로 배포
```bash
# 기존 컨테이너 중지 및 삭제 (이미 실행 중인 경우)
docker-compose down

# 새로운 이미지 빌드 및 컨테이너 실행
docker-compose up -d --build

# 실행 확인
docker ps
```

**예상 출력**:
```
CONTAINER ID   IMAGE                    PORTS                  NAMES
abc123...      cocktail_backend         0.0.0.0:80->8080/tcp   cocktail-api
def456...      postgres                 0.0.0.0:5432->5432/tcp postgres-cocktail
```

### Step 5: 로그 확인
```bash
# 컨테이너 로그 실시간 모니터링
docker logs -f cocktail-api

# 최근 100줄만 보기
docker logs --tail 100 cocktail-api

# 로그 파일 직접 확인
tail -f logs/Onz.log
```

---

## 접근 확인

### 1. Health Check (필수)
```bash
curl http://onz-cocktail.kr/onz/actuator/health
```

**예상 응답**:
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

### 2. Admin UI 접속
브라우저에서 다음 URL 접속:
- **로그 모니터링 UI**: http://onz-cocktail.kr/onz/log-viewer

### 3. 실시간 로그 스트림
```bash
curl http://onz-cocktail.kr/onz/actuator/logfile | tail -100
```

### 4. API 테스트
```bash
# 칵테일 목록 조회
curl http://onz-cocktail.kr/onz/api/v2/cocktails/best

# Swagger UI 접속 (브라우저)
http://onz-cocktail.kr/onz/swagger-ui.html
```

---

## 트러블슈팅

### 문제 1: Admin UI에 접근할 수 없음 (404 에러)

**원인**: Spring Boot Admin 설정이 누락됨

**해결**:
1. `application-docker.yaml` 파일에 Admin 설정이 있는지 확인
2. 컨테이너 재빌드: `docker-compose up -d --build`

### 문제 2: 로그 파일이 없음

**원인**: 볼륨 마운트 설정 누락

**해결**:
```bash
# docker-compose.yml에 volumes 설정 확인
volumes:
  - ./logs:/app/logs

# 로그 디렉토리 생성 및 재시작
mkdir -p logs
docker-compose restart api-server
```

### 문제 3: 데이터베이스 연결 실패

**원인**: PostgreSQL 컨테이너가 정상 실행되지 않음

**해결**:
```bash
# DB 컨테이너 로그 확인
docker logs postgres-cocktail

# DB 컨테이너 재시작
docker-compose restart db

# DB 연결 테스트
docker exec -it postgres-cocktail psql -U cocktail_user -d cocktail
```

### 문제 4: 80번 포트 접근 불가

**원인**: EC2 보안 그룹 설정 문제

**해결**:
1. AWS 콘솔 → EC2 → 보안 그룹 → 해당 인스턴스의 보안 그룹 선택
2. 인바운드 규칙 편집 → 규칙 추가
   - 유형: HTTP
   - 포트: 80
   - 소스: 0.0.0.0/0 (모든 IP)
3. 저장

### 문제 5: 도메인으로 접근 안 됨

**원인**: DNS 설정 문제

**해결**:
```bash
# DNS 전파 확인
nslookup onz-cocktail.kr

# IP로 직접 접근 테스트
curl http://your-ec2-public-ip/onz/actuator/health

# DNS 캐시 삭제 (로컬)
# Mac: sudo dscacheutil -flushcache
# Windows: ipconfig /flushdns
```

---

## 운영 환경 유지보수

### 로그 용량 관리
```bash
# 로그 디렉토리 용량 확인
du -sh logs/

# 7일 이상 된 로그 수동 삭제
find logs/ -name "Onz-*.log" -mtime +7 -delete
```

### 컨테이너 재시작
```bash
# 애플리케이션만 재시작
docker-compose restart api-server

# 전체 재시작
docker-compose restart

# 컨테이너 중지
docker-compose down

# 컨테이너 시작
docker-compose up -d
```

### 배포 업데이트
```bash
# 1. 코드 업데이트
git pull origin dev

# 2. 재빌드 및 재시작
docker-compose down
docker-compose up -d --build

# 3. 확인
docker logs -f cocktail-api
```

### 데이터베이스 백업
```bash
# PostgreSQL 백업
docker exec postgres-cocktail pg_dump -U cocktail_user cocktail > backup_$(date +%Y%m%d).sql

# 복원
cat backup_20260203.sql | docker exec -i postgres-cocktail psql -U cocktail_user cocktail
```

---

## 보안 강화 (권장)

### 1. Actuator 엔드포인트 보호

현재 Actuator가 인증 없이 노출되어 있습니다. 운영 환경에서는 다음 조치 필요:

`SecurityConfig.java` 수정:
```java
// 현재 (위험)
.requestMatchers(ACTUATOR_URLS).permitAll()

// 변경 후 (안전)
.requestMatchers("/actuator/health", "/actuator/info").permitAll()
.requestMatchers("/actuator/**", "/log-viewer/**").hasRole("ADMIN")
```

### 2. HTTPS 적용

Let's Encrypt를 사용한 무료 SSL 인증서:
```bash
# Certbot 설치
sudo apt-get update
sudo apt-get install certbot

# 인증서 발급
sudo certbot certonly --standalone -d onz-cocktail.kr

# Nginx를 사용한 리버스 프록시 설정 권장
```

### 3. 환경 변수 보안
```bash
# .env 파일 권한 제한
chmod 600 .env

# Git에서 제외 (.gitignore에 추가)
echo ".env" >> .gitignore
```

---

## 모니터링 URL 정리

| 용도 | URL |
|------|-----|
| **로그 모니터링 UI** | http://onz-cocktail.kr/onz/log-viewer |
| **실시간 로그** | http://onz-cocktail.kr/onz/actuator/logfile |
| **Health Check** | http://onz-cocktail.kr/onz/actuator/health |
| **Metrics** | http://onz-cocktail.kr/onz/actuator/metrics |
| **Swagger API 문서** | http://onz-cocktail.kr/onz/swagger-ui.html |
| **API 테스트** | http://onz-cocktail.kr/onz/api/v2/cocktails/best |

---

**작성일**: 2026-02-03
**담당자**: Backend Team
