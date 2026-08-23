# 매거진 콘텐츠 기능 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 어드민에서 JSON을 붙여넣어 매거진 글을 저장하고, 앱의 기존 뉴스 자리에서 블록으로 렌더되게 하여 로컬+터널로 end-to-end 테스트 후 Firebase로 재배포한다.

**Architecture:** `magazine_article`(V10, JSONB content/refs) 위에 Java 도메인(엔티티/리포/서비스/컨트롤러)을 `news` 도메인 패턴으로 신설. content/refs는 파싱 없이 JSONB로 통과. 어드민은 JSON 붙여넣기 → 검증 → slug upsert. 앱은 라우트 이름(`NewsScreen`/`NewsDetailScreen`)을 유지하고 데이터소스+상세 렌더러만 매거진 블록 렌더러로 교체.

**Tech Stack:** Spring Boot 3.4.1 / Java 17 / JPA(Hibernate JSONB) / Thymeleaf+htmx(admin) / React Native / axios / react-native-dotenv.

## Global Constraints

- 응답 포맷 `{code,msg,data}` (code 1=성공). 모든 JSON 필드 **snake_case** (`spring.jackson.property-naming-strategy=SNAKE_CASE`), DTO는 `@JsonProperty("snake_case")`.
- API는 `/api/v2/**` 와 `/onz/api/v2/**` 동시 노출(기존 라우팅 관례). public 엔드포인트는 JWTFilter 화이트리스트 기본 통과, 보호 시 `REQUIRE_AUTH_PATH_PATTERNS` 등록. 매거진 읽기 = public.
- 어드민은 `/admin/**` 폼 로그인 뒤에서 동작(기존 AdminNewsController 패턴).
- BE는 테스트 비활성 빌드 → 검증은 런타임(curl/어드민/시뮬레이터). 스키마 오너십: Node 아님, JPA. `prisma migrate` 금지.
- 커밋 자주. 원격 push는 명시 지시(마지막 배포 단계)에서만.
- tag.type CHECK 허용값: `FLAVOR|MOOD|BASE|GLASS` 만. 그 외 태그 타입은 스킵.
- 블록 타입: `paragraph`(text, marks[{text,style}]) · `heading`(level,text) · `quote`(text,cite) · `cocktail_spec`(name,name_en,image,specs[{k,v}],cocktail_id). 미지원 타입은 스킵.

---

## 파일 구조

**BE (Cocktail_backend/src/main/java/com/application/domain/magazine/):**
- `entity/MagazineArticle.java` — JSONB 매핑 엔티티
- `entity/MagazineArticleTag.java` — 조인 엔티티(또는 native upsert)
- `repository/MagazineArticleRepository.java`
- `dto/MagazineCard.java`, `dto/MagazineDetail.java`, `dto/MagazineListResponse.java`
- `service/MagazineService.java` — 읽기
- `controller/MagazineController.java` — `/api/v2/magazine`
- `service/AdminMagazineService.java` — 파싱/검증/upsert/tags
- `controller/AdminMagazineController.java` — `/admin/magazine`
- `templates/admin/magazine/list.html`, `templates/admin/magazine/new.html`

**FE (Cocktail_Front/src/):**
- `lib/api.ts` / `types/api.ts` — magazine 엔드포인트·타입
- `Screens/News/MagazineBlockRenderer.tsx` — 신설
- `Screens/News/NewsDetailScreen.tsx` — 매거진 상세로 교체
- `BottomTab/News/NewsScreen.tsx`, `BottomTab/Home/HomeFeedScreen.tsx`, `BottomTab/Cocktail_List/CocktailListScreen.tsx` — 소스 교체

---

## Task 1: V10 마이그레이션 커밋 (기반 고정)

**Files:**
- Modify(track): `src/main/resources/db/migration/V10__magazine.sql` (현재 untracked)

- [ ] **Step 1:** 로컬 DB에 이미 적용됨 확인: `docker exec onz-local-pg psql -U onz -d onz_local -tc "select count(*) from magazine_article;"` → 13 기대
- [ ] **Step 2:** 커밋
```bash
cd Cocktail_backend
git add src/main/resources/db/migration/V10__magazine.sql
git commit -m "chore(db): track V10 magazine migration"
```

---

## Task 2: MagazineArticle 엔티티 + 리포지토리

**Files:**
- Create: `domain/magazine/entity/MagazineArticle.java`
- Create: `domain/magazine/repository/MagazineArticleRepository.java`

**Interfaces:**
- Produces: `MagazineArticle`(getters: id, slug, title, titleLines(String json), dek, category, subcategory, coverImage, heroImage, thumbnail, imageCaption, authorName, content(String json), refs(String json), readingTimeMin, wordCount, viewCount, status, publishedAt), `MagazineArticleRepository.findByStatusOrderByPublishedAtDesc(String, Pageable)`, `findById`, `findBySlug`.

- [ ] **Step 1:** 엔티티 작성. JSONB는 문자열로 통과 매핑(변환 없음).
```java
package com.application.domain.magazine.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "magazine_article")
public class MagazineArticle {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String slug;
    private String title;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "title_lines", columnDefinition = "jsonb")
    private String titleLines;         // ["줄1","줄2"] 원본 통과
    private String dek;
    private String category;
    private String subcategory;
    @Column(name = "cover_image") private String coverImage;
    @Column(name = "hero_image") private String heroImage;
    private String thumbnail;
    @Column(name = "image_caption") private String imageCaption;
    @Column(name = "author_name") private String authorName;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb")
    private String content;            // 블록 배열 원본
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb")
    private String refs;
    @Column(name = "reading_time_min") private Integer readingTimeMin;
    @Column(name = "word_count") private Integer wordCount;
    @Column(name = "view_count") private Integer viewCount;
    @Column(name = "is_featured") private Boolean isFeatured;
    private String status;
    @Column(name = "published_at") private LocalDateTime publishedAt;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    // getters/setters
}
```
- [ ] **Step 2:** 리포지토리
```java
public interface MagazineArticleRepository extends JpaRepository<MagazineArticle, Long> {
    List<MagazineArticle> findByStatusOrderByPublishedAtDesc(String status);
    List<MagazineArticle> findByStatusAndCategoryOrderByPublishedAtDesc(String status, String category);
    Optional<MagazineArticle> findBySlug(String slug);
}
```
- [ ] **Step 3:** 컴파일 `./gradlew compileJava` → 성공
- [ ] **Step 4:** 커밋 `git commit -am "feat(magazine): entity + repository"`

---

## Task 3: 읽기 API (`/api/v2/magazine`)

**Files:**
- Create: `domain/magazine/dto/MagazineCard.java`, `MagazineDetail.java`
- Create: `domain/magazine/service/MagazineService.java`
- Create: `domain/magazine/controller/MagazineController.java`
- Test(runtime): curl

**Interfaces:**
- Consumes: `MagazineArticleRepository`
- Produces: `GET /api/v2/magazine`, `GET /api/v2/magazine/{id}`, `POST /api/v2/magazine/{id}/read`

- [ ] **Step 1:** DTO. `content`/`refs`/`title_lines`는 `@JsonRawValue String`로 원본 JSON 그대로 직렬화(재파싱 없음). tags는 List<String> label.
```java
public record MagazineDetail(
  Long id, String slug, String title,
  @JsonRawValue @JsonProperty("title_lines") String titleLines,
  String dek, String category, String subcategory,
  @JsonProperty("hero_image") String heroImage,
  @JsonProperty("cover_image") String coverImage,
  String thumbnail, @JsonProperty("image_caption") String imageCaption,
  @JsonProperty("author_name") String authorName,
  @JsonRawValue String content, @JsonRawValue String refs,
  @JsonProperty("reading_time_min") Integer readingTimeMin,
  @JsonProperty("view_count") Integer viewCount,
  @JsonProperty("published_at") LocalDateTime publishedAt,
  List<String> tags) {}
```
`MagazineCard`: id, slug, title, dek, subcategory, thumbnail, published_at, tags.
- [ ] **Step 2:** 서비스: list(category optional, status=PUBLISHED), detail(id), incrementView(id). tags는 native/JPQL로 `magazine_article_tag` join `tag.name` 조회(간단히 별도 쿼리 or `@Query`).
- [ ] **Step 3:** 컨트롤러
```java
@RestController
@RequestMapping("/api/v2/magazine")
public class MagazineController {
    @GetMapping public ResponseDto<List<MagazineCard>> list(@RequestParam(required=false) String category) {...}
    @GetMapping("/{id}") public ResponseDto<MagazineDetail> detail(@PathVariable Long id) {...}
    @PostMapping("/{id}/read") public ResponseDto<Void> read(@PathVariable Long id) {...}
}
```
- [ ] **Step 4:** 컴파일 + 로컬 재기동. 검증:
```bash
curl -s localhost:18080/onz/api/v2/magazine | head -c 300      # code:1, 목록(백필 13행)
curl -s localhost:18080/onz/api/v2/magazine/1 | head -c 300    # 상세 content 블록
```
Expected: code:1 + 데이터.
- [ ] **Step 5:** 커밋 `feat(magazine): read API`

---

## Task 4: 어드민 JSON 주입 (`/admin/magazine`)

**Files:**
- Create: `domain/magazine/service/AdminMagazineService.java`
- Create: `domain/magazine/controller/AdminMagazineController.java`
- Create: `templates/admin/magazine/list.html`, `templates/admin/magazine/new.html`
- Reference: `domain/admin/controller/AdminNewsController.java`, `templates/admin/news/*`

**Interfaces:**
- Consumes: `MagazineArticleRepository`, `TagRepository`(기존)
- Produces: `AdminMagazineService.upsertFromJson(String rawJson, boolean publish)` → 저장된 MagazineArticle

- [ ] **Step 1:** 서비스 `upsertFromJson`:
  - Jackson `ObjectMapper.readTree(raw)`.
  - 검증: `slug`,`title`,`category`,`content`(isArray) 필수 → 없으면 `IllegalArgumentException("필드 X 누락")`.
  - `category` = upper. `content`/`refs`/`title_lines`는 `node.toString()`로 원본 보존.
  - word_count = content의 각 블록 text 길이 합, reading_time_min = ceil(word_count/500) 최소1.
  - publish=true → status=PUBLISHED, published_at = json published_at || now; else DRAFT.
  - `findBySlug` → 있으면 update, 없으면 insert.
  - tags: `content` 아닌 최상위 `tags[]` 각 `{type,value,label}` → upper(type)∈{FLAVOR,MOOD,BASE,GLASS}만: `tag` where name=label upsert → `magazine_article_tag` 기존 삭제 후 재삽입.
- [ ] **Step 2:** 컨트롤러(AdminNewsController 패턴): `GET /admin/magazine`(list), `GET /admin/magazine/new`(폼), `POST /admin/magazine`(upsert 후 redirect), `POST /admin/magazine/{id}/delete`.
- [ ] **Step 3:** 템플릿 `new.html`: `<textarea name="json" rows=30>` + `<input type=checkbox name="publish" checked>` + 제출. `list.html`: 표(title/slug/status/published_at + 삭제). fragments 레이아웃 재사용.
- [ ] **Step 4:** 검증(런타임): 어드민 로그인 → `/onz/admin/magazine/new` → "리멤버 더 메인" JSON 붙여넣기 → 저장 → list 노출 → `curl /api/v2/magazine`에 등장.
- [ ] **Step 5:** 커밋 `feat(magazine): admin JSON ingestion`

---

## Task 5: FE — 매거진 API 계층 + 타입

**Files:**
- Modify: `src/lib/api.ts` (or 기존 news api 위치), `src/types/api.ts`

**Interfaces:**
- Produces: `fetchMagazineList(category?)`, `fetchMagazineDetail(id)`, `postMagazineRead(id)`, 타입 `MagazineCard`, `MagazineDetail`, `Block`.

- [ ] **Step 1:** 타입 정의(Block union: paragraph/heading/quote/cocktail_spec), MagazineDetail(content: Block[], refs, tags, hero_image, title_lines...).
- [ ] **Step 2:** API 함수 3종(`instance.get('/api/v2/magazine'...)` 등). 기존 news 함수 자리와 동일 스타일.
- [ ] **Step 3:** 타입체크 `npx tsc --noEmit` (해당 파일 무에러)
- [ ] **Step 4:** 커밋 `feat(magazine): FE api+types`

---

## Task 6: FE — MagazineBlockRenderer

**Files:**
- Create: `src/Screens/News/MagazineBlockRenderer.tsx`

**Interfaces:**
- Consumes: `Block` 타입
- Produces: `<MagazineBlockRenderer blocks={Block[]} onCocktailPress={(id)=>void} />`

- [ ] **Step 1:** 컴포넌트: blocks.map → switch(type):
  - paragraph: `<Text>` + marks(bold) 범위 분할 렌더.
  - heading: level별 스타일 `<Text>`.
  - quote: 좌측 보더 + 이탤릭 + cite.
  - cocktail_spec: 카드(Image, name/name_en, specs k:v 행), cocktail_id 있으면 `Pressable onPress=onCocktailPress(id)`.
  - default: null(스킵).
- [ ] **Step 2:** 시뮬레이터에서 임시 마운트로 시각 확인(또는 Task 7 통합 후 확인).
- [ ] **Step 3:** 커밋 `feat(magazine): block renderer`

---

## Task 7: FE — 상세 화면 교체(NewsDetailScreen → 매거진)

**Files:**
- Modify: `src/Screens/News/NewsDetailScreen.tsx`

- [ ] **Step 1:** 데이터 소스를 `fetchMagazineDetail(newsId)`로 교체(파라미터 이름 `newsId` 유지=magazine id). `postMagazineRead` fire-and-forget.
- [ ] **Step 2:** 렌더: hero_image · subcategory 칩 · title(title_lines 있으면 join('\n')) · dek · author_name + 발행일 · image_caption → `<MagazineBlockRenderer blocks={detail.content} onCocktailPress={id=>navigation.navigate('CocktailDetail'…)} />` → 하단 tags 칩 + refs.sources 링크(Linking). Markdown import 제거.
- [ ] **Step 3:** 시뮬레이터: 홈/뉴스에서 글 진입 → 블록 정상 렌더, cocktail_spec 탭 이동 확인.
- [ ] **Step 4:** 커밋 `feat(magazine): detail screen block render`

---

## Task 8: FE — 목록/홈 레일 소스 교체

**Files:**
- Modify: `src/BottomTab/News/NewsScreen.tsx`, `src/BottomTab/Home/HomeFeedScreen.tsx`, `src/BottomTab/Cocktail_List/CocktailListScreen.tsx`

- [ ] **Step 1:** 세 곳의 `/api/v2/news` 호출을 `fetchMagazineList()`로 교체. 카드 필드 매핑: image→thumbnail, title, summary→dek, category→subcategory. navigate 파라미터 `newsId`에 magazine id 전달(변경 없음).
- [ ] **Step 2:** 시뮬레이터: 홈 레일·뉴스 목록·칵테일목록 뉴스 카드가 매거진 데이터로 뜨는지 확인.
- [ ] **Step 3:** 커밋 `feat(magazine): list/home source swap`

---

## Task 9: 로컬 E2E 검증

- [ ] **Step 1:** BE 재빌드+기동: `cd Cocktail_backend && docker build -t onz-backend:local . && docker compose -f docker-compose.local.yml up -d`
- [ ] **Step 2:** 어드민에 "리멤버 더 메인" JSON 주입(Task 4 검증 재확인). `curl /api/v2/magazine/{id}` 블록 확인.
- [ ] **Step 3:** Metro 재기동 후 시뮬레이터에서: 홈 레일→상세 렌더, quote/bold/cocktail_spec, 출처 링크, 재붙여넣기 갱신.
- [ ] **Step 4:** 문제 발견 시 수정·커밋. 그린이면 다음.

---

## Task 10: Firebase 재배포 (테스터용)

**전제:** 로컬 BE + cloudflared 터널 살아있음. `.env`는 터널 URL(FE 빌드는 CI가 cd-qa.yml에서 생성).

- [ ] **Step 1:** FE 매거진 커밋들을 `Onz_Android`로 push (fast-forward): `git push origin feature/uiux-polish:Onz_Android`
- [ ] **Step 2:** cd-qa 워크플로우 실행 대기(gh run watch). android-firebase job success 확인.
- [ ] **Step 3:** `firebaseappdistribution` releaseCount 증가 + 최신 릴리스 buildVersion 확인.
- [ ] **Step 4:** 테스터 그룹 자동 배포 확인. 초대 링크는 기존 `https://appdistribution.firebase.dev/i/f500c5859fbb5f79`.
- [ ] **Step 5:** 사용자에게 완료 보고(새 빌드 번호 + 매거진 테스트 방법).

---

## Self-Review (스펙 대비)

- 스펙 §4 BE(도메인/읽기/주입) → Task 2,3,4 ✅
- 스펙 §5 FE(렌더러/상세/목록) → Task 6,7,8 ✅ / §5 API계층 → Task 5 ✅
- 스펙 §6 수용기준(주입→렌더→탭이동→재주입갱신) → Task 4,7,9 ✅
- 스펙 §3 tag 제약(4종) → Task 4 Step1 ✅
- 스펙 §8 리스크(V10 미커밋) → Task 1 ✅
- 배포 → Task 10 ✅
- 타입 일관: `fetchMagazineList/Detail/Read`, `MagazineBlockRenderer(blocks,onCocktailPress)`, DTO snake_case — Task 5/6/7 간 일치 ✅
