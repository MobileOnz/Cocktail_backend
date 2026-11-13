# Onz CSV 기반 하이브리드 검색

105개 CSV를 대상으로 구현한 선택적 검색 기능이다. 기존 칵테일 조회/검색 API, JPA 엔티티, 서비스 DB 설정은 유지한다. 실행 결과는 [검증 보고서](RESULTS.md), 검토 원문은 [review.csv](review.csv)에 있다.

## 조사 및 재사용

- 실제 백엔드는 Java 17 / Spring Boot 3.4.1 / PostgreSQL / QueryDSL / Flyway다. 기존 README의 MariaDB 설명은 오래된 내용이다.
- 기존 `cocktail.id`, 한영 이름, 이미지 URL을 재사용한다. 원본 `cocktail` 레코드를 삭제하거나 수정하는 색인 코드는 없다. 색인 전 **ID와 두 이름 모두 일치**해야 한다. 최신 이미지 URL은 조회 시 원본 테이블에서 읽는다.
- `CocktailRepositoryImpl.getCocktails`의 기존 POST `/api/v2/cocktails` 필터 검색과 `/search/history`는 그대로 둔다. 신규 공개 GET `/api/v2/cocktails/search/hybrid`는 기존 `ResponseDto`, JDBC/DataSource, JWT 인가 정책을 재사용한다.
- 이 저장소에는 기존 pgvector, RRF, LangChain/LangGraph 임베딩 코드가 없었다. 별도 `/Users/myunghunkim/marketing/onz-agent-platform/marketing-agent/platform-p2/postgres/init/01-create-hybrid-search.sql`를 확인했다. 그것은 `claims/sources`용 **384차원 지식검색**이다. 테이블이나 벡터는 공유하지 않고, 독립 `row_number` 후보 → `UNION ALL` → RRF 합산 구조를 `search/hybrid.sql`에 이식했다. 기존 SQL의 `OR / AND` 우선순위로 필터가 빠질 가능성도 공통 `filtered` CTE로 제거했다.
- 새 Java/Python 의존성은 없다. Python CSV 정규화는 표준 라이브러리, Spring JDBC/Jackson/Flyway는 기존 의존성을 쓴다. 검색 요청에서 생성형 LLM이나 유료 API를 호출하지 않는다.
- 첨부 CSV는 `/Users/myunghunkim/Downloads/onz_cocktails_105 (1).csv`에서 찾아 `data/hybrid-search/onz_cocktails_105.csv`로 복사했다. 원본 105행/26컬럼/고유 ID 105개를 검사한다.

## 데이터와 검색 규칙

- `scripts/hybrid_search/normalize.py`: UTF-8 BOM, CSV 인용/다중 줄, HTML 줄바꿈, NFKC/casefold/공백/구두점 정규화. `맛단계`, `재료_목록`은 사용하지 않는다.
- 사전은 `src/main/resources/search/dictionary.json` 한 곳에 있다. 이름 별칭, 베이스 별칭, 재료 포함 관계, 장식/불명확 항목을 명시한다. 표시 원문은 보존한다. `진`은 `진저 비어`와 부분 일치시키지 않는다.
- 재료 원문별 `optional`, `alternative`, `garnish`, `needs_review`를 JSONB 문서에 저장한다. 필수 재료 AND에는 이 플래그가 모두 false인 재료만 포함한다. 제외 필터는 선택/대체/장식 재료까지 보수적으로 제외한다. **아직 파싱하지 못한 표현의 모든 잠재적 원재료를 추론하지는 않는다.**
- 맛 원형 태그를 보존하고, `향` 또는 `플로럴`처럼 명시된 태그만 향으로 복사한다. 프로필에는 이름/베이스/맛/향/원문에서 파싱한 재료/분위기를 넣고 `유래`는 넣지 않는다. 특징 문장은 결정적인 템플릿이다.
- `content_hash`에는 표시 필드·프로필·정규화 사전 해시가 포함된다. 변경된 문서와 모델/버전 변경 문서만 다시 임베딩한다. 사전 수정은 영향을 보수적으로 반영하기 위해 전체 해시를 변경한다.
- 이름 원문 정확 > 정규화 정확 > 명시 별칭을 별도 최우선 정렬한다. 이후 키워드/코사인 후보 각각 최대 50개를 독립 순위로 계산한다. 기본 RRF는 `w_keyword/(60+rank_keyword) + w_vector/(60+rank_vector)`이고 없는 후보의 기여는 0이다. 동점은 `cocktail_id`로 정렬한다.
- `진 베이스`, `럼 없이`, `진과 레몬이 들어간`, `도수 N도 이하/이상`을 규칙으로 처리한다. 자동 해석 범위를 넘는 표현은 벡터 검색에 맡긴다. 복잡한 자연어 조건을 완전히 이해한다고 보장하지 않는다.
- `달지`, `안 달`, `덜 달`은 명시적인 드라이/쌉싸름 태그에 대한 약한 선호만 추가한다. 단맛 수치 필터와 무가당 보장은 없다.
- 도수 필터는 **전체 원본 구간이 범위 안에 들어와야** 한다: `abv_min >= abvMin AND abv_max <= abvMax`. `10도 이하`는 `abv_max <= 10`이다.
- 두 검색 채널 모두 필터를 적용한 뒤 후보를 자른다. 합성 결과도 같은 필터 집합에 조인한다. 결과 없음은 벡터로 되살리지 않는다.
- 반환 `totalCandidates`는 후보 합집합의 크기(기본 최대 100)이며 전체 카탈로그의 총 일치 건수가 아니다. `offset`은 같은 색인/모델 상태에서 결정적인 순서다. 색인 갱신 중 페이지 스냅샷 일관성은 보장하지 않는다.

## 설치와 마이그레이션

기존 PostgreSQL 서버와 같은 **메이저 버전용 pgvector 바이너리**를 먼저 설치한다. 운영 Compose의 `postgres` 이미지를 이 코드가 자동 변경하지 않는다. 현재 Compose 이미지는 pgvector 포함 여부를 보장하지 않으므로 운영 배포 전에 확인해야 한다.

로컬 예시(이번 검증에서는 Docker가 디스크 I/O 오류라 별도 임시 PostgreSQL 17을 사용했다):

```bash
brew install postgresql@17 pgvector
/opt/homebrew/opt/postgresql@17/bin/initdb -D /private/tmp/onz-hybrid-pg -A trust --locale=en_US.UTF-8
/opt/homebrew/opt/postgresql@17/bin/pg_ctl -D /private/tmp/onz-hybrid-pg \
  -l /private/tmp/onz-hybrid-pg.log -o '-h 127.0.0.1 -p 55439 -k /private/tmp' start
ollama serve
# 다른 터미널
ollama pull qwen3-embedding:0.6b
```

`trust` 설정은 위 임시 루프백 테스트 DB 전용이다. 운영 DB는 기존 인증을 유지한다. 임시 클러스터 경로가 존재하면 initdb를 반복하지 않는다.

실제 애플리케이션 DB에 적용할 때는 기존 Flyway 마이그레이션을 먼저 완료하고 연결 정보를 **명시적으로** 지정한다. 비밀번호는 환경변수/비밀 저장소로 주입한다.

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export SEARCH_JDBC_URL='jdbc:postgresql://127.0.0.1:5432/YOUR_EXISTING_DATABASE'
export SEARCH_DB_USER='YOUR_EXISTING_USER'
# SEARCH_DB_PASSWORD는 환경에서 주입
./gradlew hybridSearch --args='migrate'
python3 scripts/hybrid_search/normalize.py data/hybrid-search/onz_cocktails_105.csv
./gradlew hybridSearch --args='index --input build/hybrid-search/documents.json --dry-run'
./gradlew hybridSearch --args='index --input build/hybrid-search/documents.json --batch-size 8'
```

`migrate`는 `classpath:db/hybrid/V1__cocktail_search.sql`만 실행한다. 별도 `flyway_hybrid_search_history`를 사용하며 기존 DB의 baseline 버전은 0으로 잡는다. 기존 앱의 `db/migration/V1..V23`과 Flyway 이력을 수정하지 않는다. 검색 테이블 두 개와 인덱스, `vector`/`pg_trgm` 확장만 추가한다. 확장 생성은 권한 있는 운영자가 먼저 실행해도 된다. 기존 데이터 삭제/초기화는 없다.

- `--limit N`: 앞 N개 ID를 검사/처리한다. 전체 모델 전환을 제한 백필로 끝내면 벡터 검색은 강등된다.
- `--force-reembed`: 해시가 같아도 재임베딩한다.
- `--batch-size N`: 1~100, 기본 8. 배치별 트랜잭션으로 문서/용어를 함께 반영한다. 실패한 배치는 롤백하며 이전 완료 배치는 남아 재실행할 수 있다.
- `--dry-run`: DB ID/이름 매핑과 변경 수를 검사한다. DB 쓰기 및 모델 호출은 하지 않는다. 먼저 검색 마이그레이션은 필요하다.
- 실패는 CLI 비정상 종료와 오류/배치 ID 로그에 남는다. 원본 DB에 없는 ID를 만들어 채우지 않는다. PK/이름 불일치는 중단 후 매핑을 검토한다.

## API 활성화 및 설정

DB 준비/백필 완료 후 기존 Spring 서버 설정에 `ONZ_SEARCH_ENABLED=true`를 추가한다. 기본 false이므로 준비 전 요청은 HTTP 503이다. 기존 검색은 계속 동작한다.

| 설정 | 기본값 |
|---|---|
| `onz.search.enabled` | `false` |
| `onz.search.embedding.url` | `http://127.0.0.1:11434` |
| `onz.search.embedding.model` | `qwen3-embedding:0.6b` |
| `onz.search.embedding.version` | `onz-profile-v1` |
| `onz.search.embedding.query-prefix` | `Instruct: Retrieve cocktails matching the requested ingredients, taste and aroma.\nQuery: ` |
| `onz.search.embedding.timeout-ms` | `5000` |
| `onz.search.candidate-limit` | `50` (1~200) |
| `onz.search.rrf.k` | `60` |
| `onz.search.rrf.name-keyword-weight` | `0.85` |
| `onz.search.rrf.ingredient-keyword-weight` | `0.75` |
| `onz.search.rrf.flavor-keyword-weight` | `0.25` |
| `onz.search.rrf.mixed-keyword-weight` | `0.5` |

벡터 가중치는 `1 - keyword weight`다. 이 가중치는 초기값이며 품질 우위를 보장하지 않는다. Spring 환경변수 이름은 통상적인 대문자/밑줄 바인딩을 사용한다(복잡한 속성은 `--onz.search.embedding.timeout-ms=5000` 실행 인자로 지정할 수 있다).

CLI에는 `SEARCH_EMBEDDING_URL`, `SEARCH_EMBEDDING_MODEL`, `SEARCH_EMBEDDING_VERSION`, `SEARCH_QUERY_PREFIX`, `SEARCH_EMBEDDING_TIMEOUT_MS`를 사용한다. 배치의 기본 타임아웃은 120초다. **서버와 CLI의 모델/버전/접두사를 반드시 맞춘다.** 차원은 실제 모델 응답에서 발견하고 DB에 함께 저장하며 `vector_dims` CHECK로 검증한다. 모델/버전/차원이 다른 행이 하나라도 있으면 벡터 채널을 꺼서 혼합을 막는다.

모델 태그는 외부에서 같은 이름으로 갱신할 수 있다. 운영에서는 모델 아티팩트 digest를 고정하고 `embedding.version`에도 식별 버전을 지정해야 한다. 동일 태그의 모델을 교체하거나 질의 전처리/접두사를 바꿀 때는 버전을 올리고 전체 재색인한다. 현재 코드는 Ollama manifest digest를 자동 추적하지 않는다.

```bash
curl --get 'http://localhost:8080/onz/api/v2/cocktails/search/hybrid' \
  --data-urlencode 'query=진 베이스 상큼한 칵테일' --data 'limit=5'
curl --get 'http://localhost:8080/onz/api/v2/cocktails/search/hybrid' \
  --data-urlencode 'query=상큼한 칵테일' \
  --data-urlencode 'requiredIngredients=진' --data-urlencode 'requiredIngredients=레몬' \
  --data-urlencode 'excludedBase=럼' --data 'abvMax=30'
```

컨텍스트 경로가 없는 실행 환경에서는 `/onz`를 뺀다. 입력은 `query`(비어 있지 않은 최대 300자), `limit`(1~100), `offset`(0~10000), `base`, `excludedBase`, `requiredIngredients`, `excludedIngredients`, `abvMin`, `abvMax`, `season`, `debug`다. 배열 필터는 파라미터 반복으로 보낸다.

응답은 기존 `code/msg/data` 안에 `items`, `searchType`, 실제 `mode`, `degraded`, `warnings`, `filters`, `offset`, `limit`, `totalCandidates`, `hasMore`, 도수/단맛 정책을 담는다. item에는 기존 ID, 한영 이름, 원문/정규화 베이스, 맛·향 태그, 원문/파싱 재료, 현재 DB 이미지 URL, 근거 기반 매칭 이유가 있다. 내부 순위·RRF 점수는 `debug=true`이며 `local/dev/test`에서만 나온다. `prod/docker`가 함께 활성화된 경우도 금지한다. RRF는 확률이나 코사인 유사도가 아니다.

임베딩 타임아웃·비정상 응답·모델 불일치는 `mode=keyword`, `degraded=true`와 원인 경고를 반환한다. DB 장애/빈 색인은 HTTP 503이다. 임베딩 실패를 정상 하이브리드 결과로 위장하지 않는다.

## 재현 및 평가

```bash
export SEARCH_TEST_JDBC_URL='jdbc:postgresql://127.0.0.1:55439/postgres'
export SEARCH_TEST_DB_USER="$(id -un)"
export SEARCH_LIVE_EMBEDDING=1
bash scripts/hybrid_search/run_checks.sh
```

전용 테스트 DB에 스키마/확장을 생성할 수 있는 사용자로 실행한다. 테스트는 UUID 기반 스키마에 원본 CSV의 105개 ID/이름을 fixture로 삽입하고 **실제 검색 마이그레이션과 PostgreSQL/pgvector SQL**을 실행한 뒤 자기 스키마만 삭제한다. 운영 DB에서 실행하지 않는다. 기존 서비스 전체 회귀 테스트는 기존 H2 설정을 유지한다.

`SEARCH_LIVE_EMBEDDING`을 생략하면 결정적 mock 임베딩으로 DB/검색 계약을 검증하며 실제 모델 평가 1개만 건너뛴다. DB URL이 없으면 실행 스크립트가 실패해 DB 검증을 완료한 척하지 않는다.

산출물:

- `build/hybrid-search/documents.json`: 정규화 문서
- `build/hybrid-search/review.csv`: 수동 검토 대상 원문
- `build/hybrid-search/evaluation.json`: 25개 고정 질의 × keyword/vector/hybrid 결과, 명시적 관련 ID, top-5 hit, 이름 top-1, 필터 위반, p50/p95, 실제 로컬 모델 API 응답 샘플, exact/HNSW EXPLAIN ANALYZE
- `build/reports/tests/test/index.html`, `build/test-results/test/`: 전체 자동 테스트

평가는 이 CSV에서 사람이 명시한 소규모 smoke label이다. 독립된 사용자 relevance 평가나 운영 부하 테스트가 아니다. 각 모드 1회 워밍업 뒤 질의별 한 번 측정한다. 어휘 로드/로컬 임베딩/DB 조회를 포함한 service 호출 시간이며 HTTP 전송 시간은 제외한다. 이름은 7개 질의, 후보 검색은 25개 질의다. HNSW는 테스트 스키마에서만 생성해 비교한 뒤 삭제한다. 105건에서는 exact scan이 충분히 작아 운영 스키마에는 ANN을 추가하지 않았다.

## 운영 반영 전 남은 점검

1. 원문 검토 CSV의 약어/장식/수량 불일치를 확인한다. 파서가 원문을 임의 수정하지 않았으며 불확실한 항목은 필수 재료 필터에서 제외된다.
2. 실제 운영 칵테일 ID/한영 이름과 CSV의 대응을 dry-run으로 확인한다. 기존 DB에는 V20에서 추가한 425개 레코드가 있을 수 있으나 이번 색인은 첨부 105개만 대상이다.
3. 운영 PostgreSQL 버전·pgvector 설치·확장 권한·모델 접근 주소를 확인한다. Docker 컨테이너 내부 `127.0.0.1`은 호스트 Ollama가 아니므로 호스트 주소/네트워크를 명시한다.
4. 모델/전처리 버전을 고정하고 전체 색인 후 기능을 활성화한다. 관리자 수정 이벤트에 자동 색인을 붙이지 않았으므로 CSV 재추출/정규화/증분 색인을 운영 작업으로 실행해야 한다.
5. 공개 엔드포인트의 실제 트래픽에 맞춘 요청 제한, 모델 동시성, 콜드 스타트 타임아웃을 배포 환경에서 측정한다. 기존 메인 API는 변경하지 않았고 신규 프런트 UI는 이번 범위에 포함하지 않았다.
6. 실제 사용자 질의 라벨을 추가해 RRF 가중치와 후보 수를 조정한다. 소규모 평가에서 하이브리드가 키워드보다 좋다는 결론은 내리지 않는다.

모델/프로토콜 근거: [Qwen 공식 모델 카드](https://huggingface.co/Qwen/Qwen3-Embedding-0.6B), [Ollama 공식 API 소스/문서](https://github.com/ollama/ollama/blob/main/docs/api.md).
