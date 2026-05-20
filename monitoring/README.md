# Monitoring Configuration

이 디렉토리는 ONZ Cocktail API의 모니터링 시스템 설정 파일들을 포함합니다.

## 📁 디렉토리 구조

```
monitoring/
├── prometheus/
│   └── prometheus.yml          # Prometheus 메트릭 수집 설정
├── loki/
│   └── loki-config.yml        # Loki 로그 수집 설정
├── promtail/
│   └── promtail-config.yml    # Promtail 로그 전송 설정
└── grafana/
    └── provisioning/
        ├── datasources/
        │   └── datasources.yml     # 데이터소스 자동 설정
        └── dashboards/
            ├── dashboards.yml      # 대시보드 프로비저닝
            └── json/
                └── spring-boot-overview.json  # Spring Boot 대시보드
```

## 🚀 사용 방법

### 로컬 환경

루트 디렉토리에서:

```bash
# 기본 설정 (admin/admin)
docker-compose -f docker-compose.monitoring.yml up -d

# 커스텀 Grafana 비밀번호 사용 (권장)
GRAFANA_ADMIN_PASSWORD=your-secure-password docker-compose -f docker-compose.monitoring.yml up -d
```

**보안 권장사항:**
- 로컬 개발: 기본 `admin/admin` 사용 가능
- 프로덕션: 반드시 강력한 비밀번호로 변경
- `.env` 파일에 `GRAFANA_ADMIN_PASSWORD` 추가 가능

### 프로덕션 환경

각 설정 파일의 주석을 참고하여 엔드포인트 URL을 변경:

**prometheus.yml:**
```yaml
# 로컬
- targets: ['host.docker.internal:8080']

# 프로덕션
- targets: ['api-server:8080']  # 또는 Private IP
```

## ⚙️ 설정 파일 설명

### Prometheus (prometheus/prometheus.yml)

Spring Boot Actuator의 `/actuator/prometheus` 엔드포인트에서 메트릭을 수집합니다.

**주요 설정:**
- `scrape_interval`: 15초마다 메트릭 수집
- `targets`: 모니터링 대상 서버

### Loki (loki/loki-config.yml)

JSON 형식 로그를 저장하고 검색합니다.

**주요 설정:**
- `retention_period`: 30일 (로그 보존 기간)
- `storage`: 로컬 파일시스템 (프로덕션: S3)

### Promtail (promtail/promtail-config.yml)

애플리케이션 로그 파일을 읽어 Loki로 전송합니다.

**주요 설정:**
- `__path__`: 로그 파일 경로 (`/var/log/onz/*.log`)
- `pipeline_stages`: JSON 파싱 및 라벨 추출

### Grafana Provisioning

Grafana 시작 시 자동으로 데이터소스와 대시보드를 설정합니다.

**datasources.yml:**
- Prometheus 자동 연결
- Loki 자동 연결

**dashboards.yml:**
- `json/` 디렉토리의 대시보드 자동 로드

## 🔧 커스터마이징

### 새 대시보드 추가

1. Grafana UI에서 대시보드 생성
2. Share → Export → Save to file
3. `grafana/provisioning/dashboards/json/` 에 저장
4. Grafana 재시작 또는 자동 로드 대기 (10초)

### 메트릭 수집 주기 변경

**prometheus.yml:**
```yaml
global:
  scrape_interval: 30s  # 15s → 30s
```

### 로그 보존 기간 변경

**loki-config.yml:**
```yaml
limits_config:
  retention_period: 1440h  # 60일
```

## 📊 사용 가능한 메트릭

### JVM 메트릭
- `jvm_memory_used_bytes`: 메모리 사용량
- `jvm_gc_pause_seconds`: GC 시간
- `jvm_threads_live`: 스레드 수

### HTTP 메트릭
- `http_server_requests_seconds`: 요청 처리 시간
- `http_server_requests_seconds_count`: 요청 수

### 데이터베이스 메트릭
- `hikaricp_connections_active`: 활성 커넥션
- `hikaricp_connections_max`: 최대 커넥션

## 🐛 문제 해결

### Prometheus가 메트릭을 수집하지 못함

```bash
# Targets 상태 확인
http://localhost:9090/targets

# API 엔드포인트 직접 확인
curl http://localhost:8080/onz/actuator/prometheus
```

### Loki에 로그가 안 쌓임

```bash
# Promtail 로그 확인
docker logs promtail

# 로그 파일 경로 확인
ls -la ./logs/
```

## 📚 관련 문서

- [빠른 시작 가이드](../docs/모니터링_빠른_시작.md)
- [로컬 사용 가이드](../docs/모니터링_로컬_사용_가이드.md)
- [AWS 배포 가이드](../docs/모니터링_AWS_배포_가이드.md)

## 🔒 보안 권장사항

### 프로덕션 환경

1. **Grafana 인증 강화**
```yaml
environment:
  - GF_SECURITY_ADMIN_PASSWORD=${STRONG_PASSWORD}
  - GF_AUTH_ANONYMOUS_ENABLED=false
```

2. **Prometheus 접근 제한**
- VPC 내부에서만 접근 가능하도록 보안 그룹 설정
- Basic Auth 또는 OAuth 설정

3. **로그 민감정보 마스킹**
- Logback 설정에서 패스워드, 토큰 등 마스킹
- Promtail pipeline에서 필터링

## 📈 성능 최적화

### 고트래픽 환경

**Prometheus:**
```yaml
global:
  scrape_interval: 30s  # 간격 늘리기
  scrape_timeout: 10s

storage:
  tsdb:
    retention.time: 15d  # 보존 기간 단축
```

**Loki:**
```yaml
limits_config:
  ingestion_rate_mb: 20      # 수집 속도 제한
  max_streams_per_user: 0    # 무제한 스트림
```

## 🆘 지원

문제가 있거나 기능 요청이 있으시면:
- GitHub Issues
- Slack 채널
- 이메일: dev@onz-cocktail.kr
