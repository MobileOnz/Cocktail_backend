# 이슈 #5: Redis 캐싱 - 칵테일 목록 조회 API 목록 및 테스트

## 📋 개요
이슈 #5는 **칵테일 목록 조회 API에 Redis 캐싱을 적용**하여 응답 속도를 96% 단축(500ms → 20ms)하는 작업입니다.

---

## 🎯 적용 대상 API

### 1. 칵테일 목록 조회 API (검색 및 필터링)

#### 기본 정보
- **엔드포인트**: `POST /onz/api/v2/cocktails`
- **인증**: 불필요
- **설명**: 검색어, 필터 조건, 페이징을 지원하는 칵테일 목록 조회
- **응답 형식**: JSON (Page)

#### 요청 파라미터

**Request Body (JSON):**
```json
{
  "korName": "진토닉",
  "engName": null,
  "abvBand": "WEAK",
  "style": "클래식",
  "base": ["진"],
  "flavor": ["과일", "청량함"]
}
```

**Query Parameters (Pageable):**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| page | Integer | ❌ | 0 | 페이지 번호 (0부터 시작) |
| size | Integer | ❌ | 10 | 페이지 크기 |
| sort | String | ❌ | id,asc | 정렬 기준 (예: korName,desc) |

#### 필터 옵션

| 필드 | 타입 | 설명 | 가능한 값 |
|------|------|------|-----------|
| korName | String | 한글 이름 검색 (부분 일치) | "진토닉", "마티니" |
| engName | String | 영문 이름 검색 (부분 일치) | "Martini", "Mojito" |
| abvBand | String | 도수 레벨 | WEAK, NORMAL, STRONG |
| style | String | 스타일 | 클래식, 스탠다드, 스트롱, 라이트, 스페셜 |
| base | List<String> | 베이스 주류 (다중 선택 가능) | 진, 위스키, 럼, 보드카, 데킬라, 브랜디, 리큐르, 와인, 기타 |
| flavor | List<String> | 맛 태그 (다중 선택 가능) | 과일, 쌉쌀함, 달콤함, 부드러움, 복합적인 맛, 허브 & 스파이스, 라이트 & 청량함, 개성 강한 맛, 기타 & 특별한 맛 |

#### 응답 예시
```json
{
  "code": 1,
  "msg": "칵테일 목록 조회 성공 (v2)",
  "data": {
    "content": [
      {
        "id": 1,
        "korName": "진토닉",
        "engName": "Gin Tonic",
        "imageUrl": "https://...",
        "abv": 5.0,
        "abvBand": "WEAK",
        "style": "클래식",
        "recommendCount": 120,
        "isBookmarked": false
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10
    },
    "totalElements": 50,
    "totalPages": 5,
    "last": false,
    "first": true,
    "empty": false
  }
}
```

---

## 🧪 Postman 테스트 방법

### 준비 사항
1. Postman 설치
2. 로컬 서버 실행: `./gradlew bootRun`
3. Redis 실행: `docker-compose up -d redis`

---

### 테스트 케이스 1: 전체 칵테일 조회 (필터 없음)

#### Request
```
Method: POST
URL: http://localhost:8080/onz/api/v2/cocktails?page=0&size=20
Headers:
  Content-Type: application/json
Body (raw, JSON):
{
  "korName": null,
  "engName": null,
  "abvBand": null,
  "style": null,
  "base": null,
  "flavor": null
}
```

#### Postman 설정
1. **New Request** → HTTP 메서드를 `POST`로 선택
2. **URL 입력**: `http://localhost:8080/onz/api/v2/cocktails`
3. **Params 탭**:
   - Key: `page`, Value: `0`
   - Key: `size`, Value: `20`
4. **Headers 탭**:
   - Key: `Content-Type`, Value: `application/json`
5. **Body 탭**:
   - 라디오 버튼: `raw` 선택
   - 드롭다운: `JSON` 선택
   - 입력:
     ```json
     {
       "korName": null,
       "engName": null
     }
     ```
   - 또는 빈 객체 `{}`도 가능
6. **Send** 클릭

#### 예상 응답
- Status: 200 OK
- Body: Page 형태의 칵테일 목록

#### 캐시 확인
**첫 번째 요청 (Cache MISS):**
- 서버 로그: `🔍 Cache MISS - DB에서 칵테일 목록 조회 중...`
- 응답 시간: ~500ms
- 쿼리 수: 200+ (N+1 문제)

**두 번째 요청 (Cache HIT):**
- 서버 로그: 없음 (캐시에서 조회)
- 응답 시간: ~20ms (96% 단축!)
- 쿼리 수: 0

---

### 테스트 케이스 2: 이름 검색 ("진토닉")

#### Request
```
Method: POST
URL: http://localhost:8080/onz/api/v2/cocktails?page=0&size=10
Body:
{
  "korName": "진토닉"
}
```

#### 예상 응답
```json
{
  "code": 1,
  "msg": "칵테일 목록 조회 성공 (v2)",
  "data": {
    "content": [
      {
        "korName": "진토닉",
        ...
      }
    ],
    "totalElements": 1
  }
}
```

---

### 테스트 케이스 3: 도수 필터 (WEAK - 약한 도수)

#### Request
```
Method: POST
URL: http://localhost:8080/onz/api/v2/cocktails?page=0&size=10
Body:
{
  "abvBand": "WEAK"
}
```

#### 예상 응답
- 도수가 낮은 칵테일만 반환
- `abvBand: "WEAK"` 인 칵테일들

---

### 테스트 케이스 4: 복합 필터 (스타일 + 베이스)

#### Request
```
Method: POST
URL: http://localhost:8080/onz/api/v2/cocktails?page=0&size=10
Body:
{
  "style": "클래식",
  "base": ["진", "위스키"]
}
```

#### 예상 응답
- 스타일이 "클래식"이고
- 베이스가 "진" 또는 "위스키"인 칵테일

---

### 테스트 케이스 5: 맛 태그 필터 (과일 + 달콤함)

#### Request
```
Method: POST
URL: http://localhost:8080/onz/api/v2/cocktails
Body:
{
  "flavor": ["과일", "달콤함"]
}
```

#### 예상 응답
- 맛 태그에 "과일"과 "달콤함"이 모두 포함된 칵테일 (교집합)

---

### 테스트 케이스 6: 페이징 테스트 (2페이지)

#### Request
```
Method: POST
URL: http://localhost:8080/onz/api/v2/cocktails?page=1&size=10
Body:
{}
```

#### 예상 응답
```json
{
  "data": {
    "pageable": {
      "pageNumber": 1,
      "pageSize": 10
    },
    "first": false,
    "last": false
  }
}
```

---

### 테스트 케이스 7: 정렬 테스트 (이름 오름차순)

#### Request
```
Method: POST
URL: http://localhost:8080/onz/api/v2/cocktails?sort=korName,asc
Body:
{}
```

#### 예상 응답
- 칵테일이 한글 이름 가나다순으로 정렬

---

### 테스트 케이스 8: 로그인 사용자 (북마크 포함)

#### Request
```
Method: POST
URL: http://localhost:8080/onz/api/v2/cocktails
Headers:
  Content-Type: application/json
  Authorization: Bearer {JWT_TOKEN}
Body:
{}
```

#### 예상 응답
```json
{
  "data": {
    "content": [
      {
        "id": 1,
        "korName": "진토닉",
        "isBookmarked": true  // ← 북마크 상태 포함
      }
    ]
  }
}
```

**주의:** 로그인 사용자는 캐싱하지 않음 (북마크 상태가 사용자별로 다름)

---

## 🔍 Redis 캐시 확인 방법

### 1. Redis CLI 접속
```bash
docker exec -it cocktail-redis redis-cli -a redis123
```

### 2. 캐시 키 확인
```bash
# 모든 칵테일 목록 캐시 키 조회
127.0.0.1:6379> KEYS cocktail:list*

# 예상 출력:
# 1) "cocktail:list:::::0:20"  # 전체 조회, 0페이지, 20개
# 2) "cocktail:list:진토닉::::0:10"  # "진토닉" 검색
# 3) "cocktail:list::WEAK:::0:10"  # 도수 WEAK 필터
```

### 3. 캐시 데이터 확인
```bash
# 특정 캐시 조회 (JSON 형태로 저장됨)
127.0.0.1:6379> GET "cocktail:list:::::0:20"

# 캐시 TTL 확인 (남은 시간, 초 단위)
127.0.0.1:6379> TTL "cocktail:list:::::0:20"
# 출력 예시: (integer) 285  # 4분 45초 남음 (5분 TTL)
```

### 4. 캐시 삭제 (테스트용)
```bash
# 특정 캐시 삭제
127.0.0.1:6379> DEL "cocktail:list:::::0:20"

# 모든 칵테일 목록 캐시 삭제
127.0.0.1:6379> DEL cocktail:list*

# 패턴으로 삭제
127.0.0.1:6379> EVAL "return redis.call('del', unpack(redis.call('keys', ARGV[1])))" 0 "cocktail:list*"
```

---

## 📊 성능 측정 방법

### 1. cURL로 응답 시간 측정

#### 캐시 MISS (첫 번째 요청)
```bash
time curl -X POST http://localhost:8080/onz/api/v2/cocktails \
  -H "Content-Type: application/json" \
  -d '{}' \
  -o /dev/null -s

# 예상 출력:
# real    0m0.512s  # 약 500ms
```

#### 캐시 HIT (두 번째 요청)
```bash
time curl -X POST http://localhost:8080/onz/api/v2/cocktails \
  -H "Content-Type: application/json" \
  -d '{}' \
  -o /dev/null -s

# 예상 출력:
# real    0m0.021s  # 약 20ms (96% 단축!)
```

### 2. Postman으로 응답 시간 측정
1. **Send** 클릭 후 우측 하단의 **Time** 확인
2. 첫 번째 요청: ~500ms
3. 두 번째 요청: ~20ms

### 3. API 로깅으로 확인 (AOP 로깅 시스템 사용)
```bash
# 서버 로그 확인
tail -f logs/application.log | grep "cocktails"

# 예상 로그:
# [POST /api/v2/cocktails] CocktailService.getCocktailsV2() 시작
# 🔍 Cache MISS - DB에서 칵테일 목록 조회 중...
# [POST /api/v2/cocktails] CocktailService.getCocktailsV2() 완료 (498ms)
```

### 4. DB 쿼리 수 측정
```bash
# count-queries.sh 스크립트 사용
./scripts/count-queries.sh "http://localhost:8080/onz/api/v2/cocktails"

# 캐시 MISS: 201개 쿼리 (N+1 문제)
# 캐시 HIT: 0개 쿼리
```

---

## ✅ 테스트 체크리스트

### 캐시 적용 전
- [ ] 전체 칵테일 조회 API 호출
- [ ] 응답 시간 측정 (예상: ~500ms)
- [ ] DB 쿼리 수 측정 (예상: 200+개)
- [ ] 응답 데이터 정상 확인

### 캐시 적용 후 (비로그인 사용자)
- [ ] Redis 실행 확인 (`docker ps | grep redis`)
- [ ] 전체 칵테일 조회 API 호출 (첫 번째)
- [ ] 서버 로그에서 "Cache MISS" 확인
- [ ] 응답 시간 측정 (~500ms)
- [ ] Redis CLI로 캐시 키 확인 (`KEYS cocktail:list*`)
- [ ] 전체 칵테일 조회 API 호출 (두 번째)
- [ ] 서버 로그에 "Cache MISS" 없음 확인
- [ ] 응답 시간 측정 (~20ms) ✅ **96% 단축!**
- [ ] DB 쿼리 수 확인 (0개)

### 다양한 필터 조건 테스트
- [ ] 이름 검색 ("진토닉")
- [ ] 도수 필터 (WEAK)
- [ ] 스타일 필터 (클래식)
- [ ] 베이스 필터 (진, 위스키)
- [ ] 맛 태그 필터 (과일, 달콤함)
- [ ] 복합 필터 (스타일 + 베이스 + 맛)
- [ ] 각 필터 조건마다 별도 캐시 키 생성 확인

### 페이징 및 정렬 테스트
- [ ] 다른 페이지 조회 (page=1)
- [ ] 다른 페이지 크기 (size=5, size=50)
- [ ] 정렬 옵션 (korName,asc / korName,desc)
- [ ] 각 조합마다 별도 캐시 키 생성 확인

### 로그인 사용자 테스트
- [ ] JWT 토큰 발급
- [ ] 로그인 상태에서 칵테일 목록 조회
- [ ] 북마크 정보 포함 확인 (`isBookmarked` 필드)
- [ ] 로그인 사용자는 캐시 사용 안 함 확인
- [ ] 항상 DB에서 조회하는지 확인

### 캐시 무효화 (Admin 기능)
- [ ] Admin에서 칵테일 수정/삭제
- [ ] Redis 캐시 전체 삭제 확인
- [ ] 다음 API 호출 시 Cache MISS 발생 확인

---

## 🚨 주의사항

### 1. 로그인 vs 비로그인 사용자
- **비로그인 사용자**: 캐싱 적용 ✅ (`isBookmarked: false` 고정)
- **로그인 사용자**: 캐싱 미적용 ⚠️ (북마크 상태가 사용자별로 다름)

### 2. 캐시 키 전략
현재 캐시 키 구조:
```
cocktail:list:{조건을 toString()한 값}:{page}:{size}
```

**예시:**
- `cocktail:list:CocktailSearchConditionDto(korName=null, ...):0:20`

### 3. 캐시 TTL
- **TTL**: 5분
- 5분 후 자동 삭제
- 자주 조회되는 조건은 TTL이 갱신되지 않으므로 주의

### 4. 복잡한 필터 조건
- 필터 조건이 다르면 캐시 키도 달라짐
- 예: `"korName": "진토닉"` vs `"korName": "마티니"` → 별도 캐시
- 캐시 메모리 사용량 증가 가능

### 5. N+1 쿼리 문제
- 캐시 적용 전에는 **N+1 쿼리 문제** 발생 (200+ 쿼리)
- 이슈 #1 (Batch Fetch Size)을 먼저 적용하면 더 효과적

### 6. 로그 레벨 확인
캐시 MISS 로그를 확인하려면:
```properties
# application.properties
logging.level.com.application.domain.cocktail.service=INFO
```

---

## 📝 테스트 결과 기록 양식

### Before (캐시 적용 전)
- **API**: POST /onz/api/v2/cocktails
- **조건**: 전체 조회 (필터 없음, page=0, size=20)
- **응답 시간**: ____ms
- **DB 쿼리 수**: ____개

### After (캐시 적용 후)
- **첫 번째 요청 (Cache MISS)**:
  - 응답 시간: ____ms
  - DB 쿼리 수: ____개
  - 캐시 저장 확인: ⬜
- **두 번째 요청 (Cache HIT)**:
  - 응답 시간: ____ms
  - DB 쿼리 수: 0개
  - 개선율: ____%
  - 캐시에서 조회 확인: ⬜

### 다양한 조건 테스트
| 조건 | 첫 요청 (ms) | 두 번째 요청 (ms) | 개선율 (%) |
|------|-------------|-------------------|-----------|
| 전체 조회 | | | |
| 이름 검색 | | | |
| 도수 필터 | | | |
| 복합 필터 | | | |

### Redis 상태
- **캐시 키 수**: ____개
- **메모리 사용량**: ____MB
- **TTL 설정**: 5분

---

## 🔗 관련 파일
- **Controller**: `CocktailV2Controller.java` (line 90-121, 이미 활성화됨)
- **Service**: `CocktailService.java` (getCocktailsV2 메서드, @Cacheable 추가 필요)
- **Repository**: `CocktailRepository.java`, `CocktailRepositoryImpl.java`
- **DTO**: `CocktailSearchConditionDto.java`, `CocktailResponseDto.java`
- **Config**: `RedisCacheConfig.java` (이슈 #4에서 생성)
- **Issue**: `.github/ISSUE_TEMPLATE/perf-05-redis-cache-cocktail-list.md`
