# 실제 구현 및 검증 결과

검증일: 2026-10-23. Java 17 / PostgreSQL 17.11 / pgvector 0.8.7 / 로컬 Ollama Qwen3 0.6B. 실제 모델 응답에서 **1024차원** 확인. 운영 DB/배포 설정은 변경하지 않았으며 임시 PostgreSQL 스키마에서 실제 SQL과 API를 실행했다.

## 데이터 및 테스트

- 원본 CSV: 105행, 26컬럼, 고유 ID 105개. SHA-256 `35dc6404be139bdef03d2c075ce82a18ee78871aba23c297856a5ee87e59fe84`.
- 실제 로컬 모델 색인: 105/105 성공, 오류 0. 재실행 시 변경 없는 문서는 0건 재임베딩. 변경 1건 재색인·강제 재색인·dry-run·임베딩 실패 시 기존 레코드 보존 검증.
- 정규화 수동 검토: **9항목 / 8개 칵테일**. [원문 CSV](review.csv). 원문을 임의 수정하지 않음.
- Java 전체 회귀/통합 테스트: **54 PASS, 0 FAIL, 0 SKIP**. Python 정규화 테스트: **6 PASS**.
- 실제 DB 마이그레이션, 키워드/코사인/RRF SQL, 이름/별칭, 정확 이름 우선, 접두 검색, 복수 베이스, 필수 재료 AND, 제외/도수 필터, 후보 절단 전 필터링, 페이지 일관성, RRF 기여 0, 모델/차원 불일치, 임베딩 장애 fallback, HTTP 타임아웃, API 400/403 계약, SQL 인젝션 입력을 검증했다.
- Spring MockMvc로 실제 컨트롤러/ResponseDto 경로를 호출했다. 실제 로컬 모델과 DB로 `모히토` 요청 HTTP 200 / id=15 / mode=hybrid를 확인했다. 운영 HTTP 배포·실제 모바일 UI는 검증 범위 밖이다.

## 모드별 실측

25개 고정 smoke 질의. Top-5 성공은 사전에 지정한 관련 ID가 1개 이상 나오는 비율이다. 이름 top-1은 7개 질의다. 소규모 기능 확인용 라벨이며 독립된 사용자 품질 평가가 아니다. 워밍업 후 서비스 호출 1회씩 측정했으며 HTTP 전송 시간은 제외한다.

| 모드 | Top-5 | 이름 Top-1 | 필터 위반 | p50 | p95 |
|---|---:|---:|---:|---:|---:|
| keyword | 25/25 (100%) | 7/7 | 0 | 20.3 ms | 26.2 ms |
| vector | 23/25 (92%) | 7/7 | 0 | 135.2 ms | 151.1 ms |
| hybrid | 24/25 (96%) | 7/7 | 0 | 133.1 ms | 155.4 ms |

이 평가에서는 하이브리드가 키워드 단독보다 좋은 결과를 내지 않았다. 초기 RRF 가중치를 사용자 질의 라벨로 추가 조정해야 한다. 하이브리드 미적중 질의 `라임 없이`는 반환 결과가 라임 제외 조건은 지켰지만 좁게 지정된 관련 ID 집합에 들지 않은 경우다. 필터 준수와 관련성 라벨 적중을 분리해 기록했다.

벡터 Top-5 EXPLAIN ANALYZE: exact scan **0.476 ms**, HNSW **0.307 ms**. 105건 단일 워밍 실행 비교이며 ANN 품질/규모 벤치마크는 아니다. HNSW는 테스트 중에만 생성/삭제했고 운영 마이그레이션은 정확 선형 스캔을 유지했다.

## 하이브리드 실제 Top 5

| 질의 | 결과 (순서대로, ID) |
|---|---|
| 모히또 | 모히또 (15), 모스코 뮬 (16), 트리니다드 사워 (98), 몽키 글랜드 (69), 그래스 호퍼 (41) |
| 모히토 | 모히또 (15), 모스코 뮬 (16), 토미스 마가리타 (53), 트리니다드 사워 (98), 그래스 호퍼 (41) |
| Mojito | 모히또 (15), 모스코 뮬 (16), 마이타이 (78), 몽키 글랜드 (69), 트리니다드 사워 (98) |
| Piña Colada | 피나 콜라다 (18), 롱 아일랜드 아이스티 (77), 피스코 펀치 (95), 페르난디토 (45), 쿠바 리브레 (5) |
| Pina Colada | 피나 콜라다 (18), 롱 아일랜드 아이스티 (77), 쿠바 리브레 (5), 피스코 펀치 (95), 존 콜린스 (9) |
| 네그로니 | 네그로니 (70), 러스티 네일 (74), 홀시스 넥 (26), 트리니다드 사워 (98), 포르토 플립 (72) |
| Espresso Martini | 에스프레소 마티니 (44), 드라이 마티니 (64), 프렌치 마티니 (40), 마르티네스 (68), 폰스타 마티니 (49) |
| 화이트 럼 라임 | 다이키리 (6), 헤밍웨이 스페셜 (42), 모히또 (15), 쿠바 리브레 (5), 옐로 버드 (86) |
| 진 베이스 상큼한 칵테일 | 비스 니즈 (32), 존 콜린스 (9), 진 바질 스매시 (46), 카지노 (36), 진 피즈 (8) |
| 진저 비어 | 다크 앤 스토미 (27), 서퍼링 바스타드 (96), 모스코 뮬 (16), 드라이 마티니 (64), 카지노 (36) |
| 럼 없이 상큼한 칵테일 | 모스코 뮬 (16), 러시안 스프링 펀치 (50), 진 바질 스매시 (46), 레몬 드롭 마티니 (11), 샴페인 칵테일 (4) |
| 달지 않고 허브 향이 있는 칵테일 | 카지노 (36), 아메리카노 (1), 사우스 사이드 (51), 애비에이션 (59), 베스퍼 (85) |
| 도수 10도 이하 | 아메리카노 (1), 시 브리즈 (20), 벨리니 (2), 미모사 (14), 가리발디 (7) |
| 진과 레몬이 들어간 | 진 피즈 (8), 존 콜린스 (9), 비스 니즈 (32), 화이트 레이디 (25), 카지노 (36) |
| 민트 라임 | 모히또 (15), 올드 쿠반 (48), 마이타이 (78), 사우스 사이드 (51), 미셔너리즈 다운폴 (92) |
| 커피 | 에스프레소 마티니 (44), 아이리시 커피 (43), 블랙 러시안 (3), 쿠바 리브레 (5), 코스모폴리탄 (37) |
| 코코넛 파인애플 | 피나 콜라다 (18), 메리 픽포드 (13), 프렌치 마티니 (40), IBA 티키 (101), 정글 버드 (91) |
| 테킬라 베이스 | 마가리타 (12), 데킬라 선라이즈 (23), 팔로마 (28), 그랑 마가리타 (47), 토미스 마가리타 (53) |
| 메스칼 베이스 | 네이키드 앤 페이머스 (57), 일레갈 (90) |
| 카샤샤 베이스 | 카이피리냐 (35), 하부 지 갈루 (81) |
| 계란 흰자가 들어간 | 뉴욕 사워 (58), 클로버 클럽 (63), 라모스 피즈 (73), 피스코 사워 (80) |
| 레몬 주스 | 존 콜린스 (9), 진 피즈 (8), 레몬 드롭 마티니 (11), 비스 니즈 (32), 블러디 메리 (34) |
| 보드카 베이스 커피 | 블랙 러시안 (3), 에스프레소 마티니 (44), 레몬 드롭 마티니 (11), 섹스 온 더 비치 (21), 모스코 뮬 (16) |
| 라임 없이 | 다크 앤 스토미 (27), 화이트 레이디 (25), 브램블 (54), 피나 콜라다 (18), 존 콜린스 (9) |
| 오렌지 탄산 | 가리발디 (7), 미모사 (14), 아메리카노 (1), 진 피즈 (8), 데킬라 선라이즈 (23) |

## 대표 결과의 원문 근거

### 화이트 럼 라임

- **다이키리 (6)**: 재료_원문/dictionary: 화이트 럼 50ml (1⅔ oz); 재료_원문/dictionary: 라임 주스 25ml (⅚ oz); 맛: 라임
- **헤밍웨이 스페셜 (42)**: 재료_원문/dictionary: 화이트 럼 60ml (2 oz); 재료_원문/dictionary: 라임 주스 15ml (½ oz); 맛: 라임
- **모히또 (15)**: 재료_원문/dictionary: 화이트 럼 45ml (1½ oz); 재료_원문/dictionary: 라임 주스 20ml (⅔ oz); 재료_원문/dictionary: 라임 슬라이스; 맛: 라임
- **쿠바 리브레 (5)**: 재료_원문/dictionary: 화이트 럼 50ml (1⅔ oz); 재료_원문/dictionary: 라임 주스 10ml (⅓ oz); 재료_원문/dictionary: 라임 웨지
- **옐로 버드 (86)**: 재료_원문/dictionary: 화이트 럼 30ml (1 oz); 재료_원문/dictionary: 라임 주스 15ml (½ oz)

### 진 베이스 상큼한 칵테일

- **비스 니즈 (32)**: 베이스: 진; 재료_원문/dictionary: 진 52.5ml (1¾ oz); 맛: 상큼
- **존 콜린스 (9)**: 베이스: 진; 재료_원문/dictionary: 진 45ml (1½ oz); 맛: 상큼
- **진 바질 스매시 (46)**: 베이스: 진; 재료_원문/dictionary: 진 60ml (2 oz); 맛: 상큼
- **카지노 (36)**: 베이스: 진; 재료_원문/dictionary: 올드 톰 진 40ml (1⅓ oz)
- **진 피즈 (8)**: 베이스: 진; 재료_원문/dictionary: 진 45ml (1½ oz); 맛: 상큼

### 진과 레몬이 들어간

- **진 피즈 (8)**: 베이스: 진; 재료_원문/dictionary: 진 45ml (1½ oz); 재료_원문/dictionary: 레몬 주스 30ml (1 oz); 재료_원문/dictionary: 레몬 슬라이스
- **존 콜린스 (9)**: 베이스: 진; 재료_원문/dictionary: 진 45ml (1½ oz); 재료_원문/dictionary: 레몬 주스 30ml (1 oz); 재료_원문/dictionary: 레몬 웨지
- **비스 니즈 (32)**: 베이스: 진; 재료_원문/dictionary: 진 52.5ml (1¾ oz); 재료_원문/dictionary: 레몬 주스 22.5ml (¾ oz)
- **화이트 레이디 (25)**: 베이스: 진; 재료_원문/dictionary: 진 40ml (1⅓ oz); 재료_원문/dictionary: 레몬 주스 20ml (⅔ oz)
- **카지노 (36)**: 베이스: 진; 재료_원문/dictionary: 올드 톰 진 40ml (1⅓ oz); 재료_원문/dictionary: 레몬 주스 10ml (⅓ oz)

### 달지 않고 허브 향이 있는 칵테일

- **카지노 (36)**: 맛: 드라이
- **아메리카노 (1)**: 맛: 쌉싸름
- **사우스 사이드 (51)**: 맛: 드라이
- **애비에이션 (59)**: 맛: 드라이
- **베스퍼 (85)**: 맛: 드라이

## 파일 및 재현

- [실행·마이그레이션·운영 가이드](README.md)
- [축약 평가 JSON / 실제 API 응답](evaluation.json)
- 원본 상세 평가/SQL 계획: `build/hybrid-search/evaluation.json`
- 테스트 로그: `build/hybrid-search/checks.log`, HTML: `build/reports/tests/test/index.html`
- 재현: README 환경변수 설정 후 `bash scripts/hybrid_search/run_checks.sh`; 이후 `python3 scripts/hybrid_search/report.py`.

수정/추가 파일:

- `README.md`
- `build.gradle`
- `data/hybrid-search/onz_cocktails_105.csv`
- `docs/hybrid-search/README.md`
- `docs/hybrid-search/RESULTS.md`
- `docs/hybrid-search/evaluation.json`
- `docs/hybrid-search/review.csv`
- `scripts/hybrid_search/.gitignore`
- `scripts/hybrid_search/normalize.py`
- `scripts/hybrid_search/report.py`
- `scripts/hybrid_search/run_checks.sh`
- `scripts/hybrid_search/test_normalize.py`
- `src/main/java/com/application/common/auth/config/EndpointAuthorizationPolicy.java`
- `src/main/java/com/application/domain/cocktail/search/EmbeddingProvider.java`
- `src/main/java/com/application/domain/cocktail/search/HybridSearchController.java`
- `src/main/java/com/application/domain/cocktail/search/HybridSearchRequest.java`
- `src/main/java/com/application/domain/cocktail/search/HybridSearchService.java`
- `src/main/java/com/application/domain/cocktail/search/OllamaEmbeddingProvider.java`
- `src/main/java/com/application/domain/cocktail/search/SearchDictionary.java`
- `src/main/java/com/application/domain/cocktail/search/SearchIndexCli.java`
- `src/main/java/com/application/domain/cocktail/search/SearchIndexer.java`
- `src/main/java/com/application/domain/cocktail/search/SearchQuery.java`
- `src/main/resources/db/hybrid/V1__cocktail_search.sql`
- `src/main/resources/search/dictionary.json`
- `src/main/resources/search/hybrid.sql`
- `src/test/java/com/application/contract/EndpointAuthorizationContractTest.java`
- `src/test/java/com/application/domain/cocktail/search/HybridSearchIntegrationTest.java`
- `src/test/java/com/application/domain/cocktail/search/OllamaEmbeddingProviderTest.java`
- `src/test/java/com/application/domain/cocktail/search/SearchEvaluation.java`

기존 사용자 변경 `.gitignore`와 제공된 구현 프롬프트 파일은 보존했으며 이번 구현에서 수정하지 않았다.

## 남은 문제와 운영 반영 순서

1. 검토 9항목은 원문 확인이 필요하다. 특히 드라이 베르무트 수량 누락, 스위트 베르 약어, 130ml/1oz 불일치, 일부 장식 판정은 미확정이다.
2. 첨부 105개만 색인했다. 운영 DB의 다른 칵테일과 관리자 수정 자동 동기화는 별도 백필/운영 작업이 필요하다.
3. 모델 태그 변경을 자동 감지하는 digest 추적은 없다. 운영 모델 digest와 embedding.version을 고정하고 변경 시 전체 재색인해야 한다.
4. 운영 PostgreSQL에 pgvector 설치·권한 확인 → 마이그레이션 → ID/이름 dry-run → 전체 색인 → ONZ_SEARCH_ENABLED=true → 실제 운영 HTTP/앱 호출 순으로 적용한다. 기존 Docker는 디스크 I/O 오류라 이번 실행에 사용하지 못했다.
5. 대규모 데이터·동시 요청 부하·콜드 스타트는 미측정이다. 자연어 규칙은 명시된 패턴에 한정되며 단맛 보장은 없다.
