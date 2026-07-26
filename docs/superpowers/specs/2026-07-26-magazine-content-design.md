# 매거진(Magazine) 콘텐츠 기능 설계

- 작성일: 2026-07-26
- 상태: 설계 확정 대기 → writing-plans 전환 예정
- 관련 스키마: `Cocktail_backend/.../db/migration/V10__magazine.sql` (테이블만 존재, 도메인 코드·UI 미착수)

## 1. 목적 / 배경

파편화된 글 콘텐츠(news / cocktail_news / guide_detail)를 대체할 **단일 정본 콘텐츠 모델**. 본문은 타입별 블록 배열(JSONB)로 저장해 새 블록 타입 추가 시 마이그레이션이 필요 없다. 칵테일 스펙은 복사하지 않고 `cocktail_id`로 마스터를 참조한다.

현 상태: `magazine_article` / `magazine_article_tag` 테이블은 존재(V10, 로컬 적용됨, 뉴스 백필 13행)하나 **Java 도메인·읽기 API·주입 경로·앱 렌더러가 전부 없음.** 앱 뉴스 상세는 마크다운(`react-native-markdown-display`)이라 블록을 못 그린다.

목표: 어드민에서 **JSON을 붙여넣어** 매거진 글을 저장하고, 앱의 **기존 뉴스 자리**에서 블록으로 렌더되는 것까지 로컬+터널 세팅에서 end-to-end 테스트.

## 2. 확정된 결정 (brainstorming)

| 항목 | 결정 |
|---|---|
| 주입 방식 | **어드민 JSON 붙여넣기** → 서버 검증 후 slug 기준 upsert (항목별 편집 폼은 백로그) |
| 앱 배치 | **기존 뉴스 진입점을 매거진으로 대체** (라우트 이름 `NewsScreen`/`NewsDetailScreen` 유지, 내부 데이터소스+상세 렌더러만 교체 → 홈/칵테일목록 링크 무변경) |
| 태그 | **MVP 포함** (기존 `tag` 테이블 재사용 + `magazine_article_tag` 조인 주입, 칩 렌더) |
| news/guide 원본 | 이번엔 흡수/삭제하지 않음. 매거진은 news 백필분 + 신규 글을 읽음 |

## 3. 데이터 모델 (기존 V10 그대로 사용)

`magazine_article`: id, slug(unique), title, title_lines(JSONB), dek, category(STORY|MOOD|BASE|SEASON), subcategory, cover_image, hero_image, thumbnail, image_caption, author_name/bio/avatar, **content JSONB(블록 배열)**, **refs JSONB**({related_article_slugs, sources, cocktail_ids}), reading_time_min, word_count, view_count, is_featured, status(DRAFT|PUBLISHED), published_at, created_at, updated_at.

`magazine_article_tag`: (article_id, tag_id) 조인. `tag` 재사용.

**⚠️ tag 제약**: 실제 `tag` 테이블 컬럼은 `id / name(unique) / type`뿐이며 `type` CHECK가 `FLAVOR|MOOD|BASE|GLASS`만 허용(V10 주석의 CITY/NEIGHBORHOOD/SEASON/OCCASION은 실제로는 들어가지 않음). 입력 JSON 태그 `{type,value,label}`는 `tag.name = label`, `tag.type = upper(type)`로 매핑하되 **허용 4종이 아니면 스킵**한다. (이 아티클 태그는 BASE/MOOD라 통과)

블록 타입(입력 JSON 기준): `paragraph`(text, marks[{text,style:"bold"}]), `heading`(level, text), `quote`(text, cite), `cocktail_spec`(name, name_en, image, specs[{k,v}], cocktail_id nullable). 그 외 타입은 렌더 시 조용히 스킵.

## 4. 백엔드 설계

### 4.1 도메인 신설 (`NewsController` 패턴 복제)
`domain/magazine/` 아래 entity / repository / service / controller / dto.
- `MagazineArticle` 엔티티: JSONB 컬럼은 Hibernate `@JdbcTypeCode(SqlTypes.JSON)` + String/JsonNode 매핑(변환 없이 통과, snake_case 유지).
- content/refs/title_lines는 **파싱하지 않고 원본 JSON 그대로 저장·반환**(스키마 유연성 원칙).

### 4.2 읽기 API (public)
- `GET /api/v2/magazine?category=&page=` — 발행분(status=PUBLISHED) 목록, published_at DESC. 카드 DTO: id, slug, title, dek, subcategory, thumbnail, published_at, tags[label].
- `GET /api/v2/magazine/{id}` — 상세: 전체 필드 + content(블록)+refs+tags. `title_lines` 포함.
- `POST /api/v2/magazine/{id}/read` — view_count++ (fire-and-forget, 실패 무시).
- `/onz` 접두 동시 노출은 기존 라우팅 관례 따름. JWTFilter 화이트리스트 = public.

### 4.3 어드민 JSON 주입 (`AdminNewsController` 패턴)
- `GET /admin/magazine` — 목록(제목·slug·status·published_at·수정/삭제).
- `GET /admin/magazine/new` — **JSON 붙여넣기 화면**(큰 textarea + "발행" 체크박스, 기본 on).
- `POST /admin/magazine` — 본문 JSON 파싱·검증:
  - 필수: slug, title, category, content(배열). category는 upper 정규화(STORY 등).
  - word_count = content 내 text 길이 합산 → reading_time_min 계산.
  - published 체크 시 status=PUBLISHED + published_at(입력값 또는 now), 아니면 DRAFT.
  - **slug 기준 upsert**(존재하면 update, 없으면 insert) → 같은 글 재붙여넣기로 갱신 가능.
  - tags: JSON `tags[]` → 각 `{type,value,label}`를 tag(name=label,type=upper) upsert(허용 4종만) → 기존 조인 삭제 후 재삽입.
- 검증 실패 시 어드민 화면에 에러 메시지(어떤 필드/이유).

## 5. 앱(FE) 설계 — 라우트 이름 유지, 내부 교체

- **목록/홈 레일**: `NewsScreen`, `HomeFeedScreen` 가로 레일, `CocktailListScreen`의 뉴스 카드가 `/api/v2/news` → `/api/v2/magazine`로 소스 교체. 카드 필드 매핑(thumbnail/title/dek/subcategory). 파라미터는 기존 `newsId`에 magazine id(숫자) 그대로 전달.
- **상세**: `NewsDetailScreen`이 `/api/v2/magazine/{id}` 호출. Markdown 제거하고 **`MagazineBlockRenderer` 신설**:
  - `paragraph`: 텍스트 + `marks` 범위 bold 처리.
  - `heading`: level별 스타일.
  - `quote`: 인용 스타일 + cite.
  - `cocktail_spec`: 스펙 카드(이미지·name/name_en·specs k:v). `cocktail_id` 있으면 탭 시 칵테일 상세로 이동.
  - 상단: hero_image · subcategory 칩 · title(title_lines 있으면 줄바꿈) · dek · author_name + 발행일 · image_caption.
  - 하단: `refs.sources[]` → "출처" 링크 목록(외부 브라우저).
  - 태그 칩: tags[label] 렌더.
  - 알 수 없는 블록 타입은 스킵.
- 조회수: 진입 시 `POST /api/v2/magazine/{id}/read` fire-and-forget.
- 이미지 URL이 `http://onz-cocktail.kr/...` 절대경로여도 그대로 로드(현 .env는 터널이지만 이미지 호스트는 별개). null/누락 시 영역 생략.

## 6. 테스트 방법 (수용 기준)

1. 어드민(`localhost:18080/onz/admin/magazine/new`)에 "리멤버 더 메인" JSON 붙여넣기 → 저장 성공, 목록에 노출.
2. `GET /api/v2/magazine` 응답에 해당 글 포함(code:1, tags 포함), `GET /api/v2/magazine/{id}`에 블록 content 반환.
3. 앱(시뮬레이터 또는 Firebase 배포 실기기, 터널 on)에서 뉴스 진입점 → 목록에 글 표시 → 상세에서 paragraph/heading/quote/cocktail_spec/bold/출처가 올바르게 렌더.
4. cocktail_spec 카드 탭 → 해당 칵테일 상세로 이동(cocktail_id 있는 경우).
5. 같은 JSON 재붙여넣기 → 중복 생성 없이 갱신(slug upsert).

## 7. YAGNI (이번 범위 제외 · 백로그)

- 항목별 입력/편집 폼(블록 에디터), DRAFT 미리보기.
- `refs.related_article_slugs` 연관글 이동(슬러그 라우팅).
- 태그 필터 탭 UI 세분화, CITY/SEASON 등 tag.type 확장(CHECK 제약 변경 필요).
- news/cocktail_news/guide_detail 원본 마이그레이션·제거, 라우트/컴포넌트 Magazine 리네이밍.
- 프로덕션 배포(현재는 로컬+터널 검증까지).

## 8. 리스크 / 주의

- V10 마이그레이션이 **BE 레포에 untracked(미커밋)** 상태 → 스펙 확정 시 함께 커밋 필요.
- content를 파싱하지 않고 통과시키므로 **주입 시 검증이 유일한 방어선**(필수 필드·category·블록 타입 화이트리스트).
- 앱은 새 빌드가 있어야 매거진이 보임(현재 Firebase 배포본은 매거진 이전 코드). 시뮬레이터 우선 검증 후 필요 시 재배포.
