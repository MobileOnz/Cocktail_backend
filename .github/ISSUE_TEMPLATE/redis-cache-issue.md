---
name: Redis 캐시 적용
about: 칵테일 조회 API 성능 향상을 위한 Redis 캐시 도입
title: "[feat]: Redis 캐시를 통한 칵테일 조회 성능 향상"
labels: 'enhancement, performance'
assignees: ''

---

[//]: # (feat: 새로운 기능)
[//]: # (refactor: 기존 기능 자체는 변하지 않고 코드 개선 :: 로직만 변경)
[//]: # (chore: 빌드 설정, 패키지 매니저 설정 등, 주석제거 등 분류하기 어려운 자잘한 수정에 대한 커밋)
[//]: # (fix: 버그 수정)
[//]: # (style: 포맷팅, 세미콜론 누락, lint 수정 등)
[//]: # (docs: 문서 추가 또는 수정)
[//]: # (test: 테스트 코드)
[//]: # (hotfix: 급한 수정 사항 반영 시 사용)

## 📌 기능 설명
칵테일 조회 API의 응답 성능을 향상시키기 위해 Redis 캐시를 도입합니다.
- 현재 Caffeine 인메모리 캐시 사용 중 → Redis로 확장하여 분산 환경 대응
- 태그 기반 검색, 필터링 등 복잡한 QueryDSL 쿼리 결과를 캐싱
- 읽기 성능 최적화 및 DB 부하 감소

## ✅ To-Do 리스트

### 1. 환경 설정
- [ ] Redis 의존성 추가 (build.gradle - spring-boot-starter-data-redis)
- [ ] Redis 연결 설정 (application.properties - host, port, password)
- [ ] RedisTemplate 및 CacheManager 설정 클래스 작성
- [ ] Redis 직렬화 전략 설정 (JSON, Jackson2JsonRedisSerializer)

### 2. 캐시 전략 설계
- [ ] 캐시 적용 대상 API 선정
  - 칵테일 목록 조회 (태그, 필터 조건별)
  - 칵테일 상세 조회
  - 인기 칵테일 / 추천 칵테일
- [ ] 캐시 키 네이밍 규칙 정의 (예: `cocktail:list:{filters}`, `cocktail:detail:{id}`)
- [ ] TTL(Time-To-Live) 설정 전략 결정
  - 목록 조회: 5-10분
  - 상세 조회: 10-30분
  - 추천: 1시간

### 3. 코드 구현
- [ ] `common/cache/RedisCacheConfig` 클래스 작성
- [ ] CacheType enum에 Redis 캐시 타입 추가
- [ ] CocktailService에 `@Cacheable`, `@CachePut`, `@CacheEvict` 어노테이션 적용
- [ ] 커스텀 캐시 키 생성 로직 구현 (복잡한 검색 조건 처리)

### 4. 캐시 무효화 전략
- [ ] 칵테일 생성/수정/삭제 시 관련 캐시 무효화 로직 추가
- [ ] 태그 변경 시 연관된 칵테일 캐시 무효화
- [ ] 북마크/리액션 변경 시 캐시 갱신 전략 검토

### 5. 인프라 및 배포
- [ ] Docker Compose에 Redis 컨테이너 추가
- [ ] EC2 환경에 Redis 설치 또는 AWS ElastiCache 연동 검토
- [ ] 환경변수 설정 (REDIS_HOST, REDIS_PORT, REDIS_PASSWORD)

### 6. 모니터링 및 테스트
- [ ] 캐시 히트율 모니터링 로깅 추가
- [ ] 성능 테스트 (캐시 적용 전/후 응답 시간 비교)
- [ ] 부하 테스트 (동시 요청 처리 성능 검증)
- [ ] Redis 메모리 사용량 모니터링

### 7. 문서화
- [ ] CLAUDE.md에 Redis 캐시 사용 방법 추가
- [ ] README.md에 Redis 설정 방법 및 환경변수 안내
- [ ] 캐시 전략 및 키 규칙 문서화

## 💡 기타
**기술 스택**
- Spring Boot Data Redis
- Lettuce (기본 Redis 클라이언트)
- Jackson2JsonRedisSerializer (JSON 직렬화)

**고려 사항**
- 기존 Caffeine 캐시와의 공존 전략 (L1: Caffeine, L2: Redis 멀티 레벨 캐싱 검토)
- Redis Cluster vs Standalone 선택
- 캐시 워밍(Cache Warming) 전략
- 캐시 stampede 방지 (동시 다발적 캐시 미스 시 DB 과부하)

**성능 목표**
- 칵테일 목록 조회 응답 시간 50% 이상 단축
- 동시 요청 100+ 처리 시 안정적인 성능 유지
- DB 쿼리 수 30% 이상 감소
