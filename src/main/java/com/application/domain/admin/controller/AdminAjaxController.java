package com.application.domain.admin.controller;

import com.application.common.exception.custom.CustomApiException;
import com.application.common.response.ResponseDto;
import com.application.common.storage.ImageStorage;
import com.application.domain.cocktail.controller.CocktailV2Controller;
import com.application.domain.cocktail.dto.IngredientDto;
import com.application.domain.cocktail.dto.TagDto;
import com.application.domain.cocktail.dto.TagGroupDto;
import com.application.domain.cocktail.entity.Cocktail;
import com.application.domain.cocktail.entity.CocktailTag;
import com.application.domain.cocktail.entity.Ingredient;
import com.application.domain.cocktail.entity.Tag;
import com.application.domain.cocktail.enums.Season;
import com.application.domain.cocktail.enums.TagType;
import com.application.domain.cocktail.repository.CocktailRepository;
import com.application.domain.cocktail.repository.TagRepository;
import com.application.domain.cocktail.service.CocktailService;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 관리자 화면(Thymeleaf)에서 호출되는 AJAX 엔드포인트 모음.
 *
 * 보안: /admin/** 은 SecurityConfig 의 adminSecurity 체인(form-login + 세션) 으로 보호되므로
 *      별도 인증 코드 없이 동작. CSRF 는 admin 체인에서 disable 되어 있음.
 *
 * 응답 포맷: 공통 ResponseDto<T> ({code, msg, data}) 사용. JSON 필드는 snake_case 로 직렬화하기 위해
 *          DTO 에 @JsonProperty 명시 (전역 NamingStrategy 가 설정되어 있지 않음).
 *
 * 이미지 업로드/삭제 두 엔드포인트는 S3 자격증명이 환경변수로 주입되지 않으면 동작 불가하므로,
 * 503 + code:-1 stub 으로 명확히 응답한다. 실제 구현은 AWS_S3_ACCESS_KEY / AWS_S3_SECRET_KEY / S3_BUCKET
 * 가 주입되는 시점에 작성한다.
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminAjaxController {

    private final CocktailService cocktailService;
    private final CocktailRepository cocktailRepository;
    private final TagRepository tagRepository;
    private final EntityManager entityManager;
    private final ImageStorage imageStorage;

    /* =========================================================================================
     *  1) GET /admin/search/cocktail
     *     - cocktails.html 의 ag-grid 채우기
     *     - 응답 row 의 필드명은 cocktails.html columnDefs 의 field 값과 정확히 일치해야 한다.
     *       (id, cocktail_kr, cocktail_en, abv_band, taste_level, createdAt)
     * ========================================================================================= */
    @GetMapping("/search/cocktail")
    public ResponseEntity<ResponseDto<List<AdminCocktailRow>>> searchCocktail(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "100") int size,
            @RequestParam(value = "cocktailName", required = false) String cocktailName
    ) {
        // 가장 단순한 방식: findAll + 메모리 필터.
        // (대부분 100 건 내외 칵테일이며, 관리자 화면이라 부하가 거의 없음.)
        List<Cocktail> all = cocktailRepository.findAll();

        List<Cocktail> filtered;
        if (cocktailName != null && !cocktailName.isBlank()) {
            String needle = cocktailName.toLowerCase(Locale.ROOT);
            filtered = all.stream()
                    .filter(c -> {
                        String kor = c.getKorName() != null ? c.getKorName() : c.getCocktailKR();
                        String eng = c.getEngName() != null ? c.getEngName() : c.getCocktailEN();
                        boolean korHit = kor != null && kor.toLowerCase(Locale.ROOT).contains(needle);
                        boolean engHit = eng != null && eng.toLowerCase(Locale.ROOT).contains(needle);
                        return korHit || engHit;
                    })
                    .toList();
        } else {
            filtered = all;
        }

        // 페이징 (관리자 화면은 default size=100 이므로 사실상 단일 페이지)
        int from = Math.min(page * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        List<Cocktail> pageContent = filtered.subList(from, to);

        List<AdminCocktailRow> rows = pageContent.stream()
                .map(AdminCocktailRow::from)
                .toList();

        return ResponseEntity.ok(ResponseDto.onSuccess("칵테일 목록 조회 성공", rows));
    }

    /* =========================================================================================
     *  2) GET /admin/cocktail/info?cocktailId=X
     *     - cocktail/detail.html fillForm() 이 기대하는 필드:
     *       cocktail_kr, cocktail_en, max_alcohol, min_alcohol, abv_band, taste_level,
     *       origin_text, image_url, seasons (string[]), ingredients ([{name, amount}]),
     *       tags ([{type, tags:[{id, name}]}])
     * ========================================================================================= */
    @GetMapping("/cocktail/info")
    public ResponseEntity<ResponseDto<AdminCocktailDetail>> cocktailInfo(
            @RequestParam("cocktailId") Long cocktailId
    ) {
        Cocktail cocktail = cocktailRepository.findById(cocktailId)
                .orElseThrow(() -> new CustomApiException("칵테일이 존재하지 않습니다."));

        return ResponseEntity.ok(ResponseDto.onSuccess("칵테일 상세 조회 성공", AdminCocktailDetail.from(cocktail)));
    }

    /* =========================================================================================
     *  3) GET /admin/cocktail/tags
     *     - tag.html / cocktail/detail.html 모두 사용
     *     - 응답 형태: { "FLAVOR":[{id,name},...], "MOOD":[...], "BASE":[...], "GLASS":[...] }
     *     - CocktailService.getCocktailTags(null) 가 그대로 이 형태를 반환 (Map<String, Object>)
     * ========================================================================================= */
    @GetMapping("/cocktail/tags")
    public ResponseEntity<ResponseDto<Map<String, Object>>> cocktailTags() {
        Map<String, Object> tags = cocktailService.getCocktailTags(null);
        return ResponseEntity.ok(ResponseDto.onSuccess("태그 조회 성공", tags));
    }

    /* =========================================================================================
     *  4) GET /admin/manage/tag
     *     - tag.html 의 태그 목록 화면
     *     - 응답 형태는 #3 와 동일 (Object.entries(allTags).forEach 로 그룹별 렌더링)
     *     - JS 가 이 동일 path 에 ?id= 쿼리로 "삭제 요청" 도 보내므로(아래 deleteTag 참고) 분리 처리한다.
     * ========================================================================================= */
    @GetMapping("/manage/tag")
    public ResponseEntity<ResponseDto<?>> manageTagGetOrDelete(
            @RequestParam(value = "id", required = false) Long deleteId
    ) {
        if (deleteId != null) {
            // tag.html 의 deleteTag() 가 GET /admin/manage/tag?id=X 로 요청한다. (의도적인 RPC-over-GET)
            return deleteTagInternal(deleteId);
        }
        Map<String, Object> tags = cocktailService.getCocktailTags(null);
        return ResponseEntity.ok(ResponseDto.onSuccess("태그 목록 조회 성공", tags));
    }

    /* =========================================================================================
     *  5) POST /admin/manage/tag
     *     - tag.html addTag() 로부터 호출. body: { type: "FLAVOR", name: "달콤" }
     * ========================================================================================= */
    @PostMapping("/manage/tag")
    @Transactional
    public ResponseEntity<ResponseDto<TagDto>> createTag(@RequestBody TagSaveReq req) {
        if (req == null || req.getName() == null || req.getName().isBlank()
                || req.getType() == null || req.getType().isBlank()) {
            return ResponseEntity.ok(ResponseDto.onFail(-1, "태그 type/name 누락"));
        }
        TagType type;
        try {
            type = TagType.valueOf(req.getType().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.ok(ResponseDto.onFail(-1, "지원하지 않는 태그 타입: " + req.getType()));
        }

        // 동일 (type, name) 중복 방지: name unique 제약 + 동일 type 인 경우 멱등성 보장
        Tag existing = tagRepository.findByType(type).stream()
                .filter(t -> t.getName().equals(req.getName().trim()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            return ResponseEntity.ok(ResponseDto.onSuccess("이미 존재하는 태그", new TagDto(existing.getId(), existing.getName())));
        }

        Tag saved = tagRepository.save(new Tag(type, req.getName().trim()));
        return ResponseEntity.ok(ResponseDto.onSuccess("태그 생성 성공", new TagDto(saved.getId(), saved.getName())));
    }

    /* =========================================================================================
     *  6) GET /admin/cocktails/delete?cocktailId=X
     *     - cocktails.html 의 row 삭제 버튼 + cocktail/detail.html 의 삭제 버튼이 사용
     *     - 칵테일 엔티티에 soft-delete 컬럼이 없으므로 hard-delete 수행
     * ========================================================================================= */
    @GetMapping("/cocktails/delete")
    @Transactional
    public ResponseEntity<ResponseDto<Void>> deleteCocktail(@RequestParam("cocktailId") Long cocktailId) {
        if (!cocktailRepository.existsById(cocktailId)) {
            return ResponseEntity.ok(ResponseDto.onFail(-1, "존재하지 않는 칵테일입니다. id=" + cocktailId));
        }
        try {
            cocktailRepository.deleteById(cocktailId);
            entityManager.flush();
            return ResponseEntity.ok(ResponseDto.onSuccess("삭제되었습니다.", null));
        } catch (Exception ex) {
            log.error("[admin] cocktail delete failed id={} : {}", cocktailId, ex.getMessage(), ex);
            return ResponseEntity.ok(ResponseDto.onFail(-1, "삭제 실패: 외래키 제약 등으로 인해 삭제할 수 없습니다."));
        }
    }

    /* =========================================================================================
     *  내부: 태그 삭제 (manage/tag?id=X 에서 호출)
     * ========================================================================================= */
    @Transactional
    public ResponseEntity<ResponseDto<?>> deleteTagInternal(Long tagId) {
        if (!tagRepository.existsById(tagId)) {
            return ResponseEntity.ok(ResponseDto.onFail(-1, "존재하지 않는 태그입니다. id=" + tagId));
        }
        try {
            tagRepository.deleteById(tagId);
            entityManager.flush();
            return ResponseEntity.ok(ResponseDto.onSuccess("태그 삭제 성공", null));
        } catch (Exception ex) {
            log.error("[admin] tag delete failed id={} : {}", tagId, ex.getMessage(), ex);
            return ResponseEntity.ok(ResponseDto.onFail(-1, "삭제 실패: 칵테일에 연결된 태그입니다."));
        }
    }

    /* =========================================================================================
     *  이미지 업로드/삭제: ImageStorage 추상화 사용.
     *  - AWS 자격증명 없으면 LocalFileImageStorage (./uploads), 있으면 S3ImageStorage 자동 선택.
     *  - cocktail/detail.html 클라이언트는 response.data 에서 URL 문자열을 읽어 input/preview 에 세팅.
     * ========================================================================================= */
    @PostMapping("/cocktail/image/upload")
    public ResponseEntity<ResponseDto<String>> uploadCocktailImage(@RequestParam("file") MultipartFile file) {
        try {
            String url = imageStorage.upload("cocktails", file);
            return ResponseEntity.ok(ResponseDto.onSuccess("업로드 성공 (" + imageStorage.mode() + ")", url));
        } catch (Exception ex) {
            log.error("[admin] cocktail image upload failed: {}", ex.getMessage(), ex);
            return ResponseEntity.ok(ResponseDto.onFail(-1, "업로드 실패: " + ex.getMessage()));
        }
    }

    @PostMapping("/cocktail/image/delete")
    public ResponseEntity<ResponseDto<Boolean>> deleteCocktailImage(@RequestBody ImageDeleteReq req) {
        try {
            boolean deleted = imageStorage.delete(req.getUrl());
            return ResponseEntity.ok(ResponseDto.onSuccess("삭제 처리 완료 (" + imageStorage.mode() + ")", deleted));
        } catch (Exception ex) {
            log.error("[admin] cocktail image delete failed: {}", ex.getMessage(), ex);
            return ResponseEntity.ok(ResponseDto.onFail(-1, "삭제 실패: " + ex.getMessage()));
        }
    }

    @Getter @Setter
    public static class ImageDeleteReq {
        private String url;
    }

    /* ======================================== DTO ======================================== */

    /**
     * 칵테일 목록 한 행 (ag-grid). 필드명은 cocktails.html columnDefs 의 field 와 일치해야 한다.
     */
    @Getter
    @AllArgsConstructor
    public static class AdminCocktailRow {
        private final Long id;

        @JsonProperty("cocktail_kr")
        private final String cocktailKr;

        @JsonProperty("cocktail_en")
        private final String cocktailEn;

        @JsonProperty("abv_band")
        private final String abvBand;

        @JsonProperty("taste_level")
        private final String tasteLevel;

        @JsonProperty("createdAt")
        private final LocalDateTime createdAt;

        public static AdminCocktailRow from(Cocktail c) {
            String kor = c.getKorName() != null ? c.getKorName() : c.getCocktailKR();
            String eng = c.getEngName() != null ? c.getEngName() : c.getCocktailEN();
            String abv = c.getAbvBand() != null ? c.getAbvBand().name() : null;
            String taste = c.getTasteLevel() != null ? c.getTasteLevel().name() : null;
            return new AdminCocktailRow(c.getId(), kor, eng, abv, taste, c.getCreatedAt());
        }
    }

    /**
     * 칵테일 상세 (편집 화면). 필드명은 detail.html fillForm() 이 읽는 키와 일치.
     */
    @Getter
    @AllArgsConstructor
    public static class AdminCocktailDetail {
        @JsonProperty("id")
        private final Long id;

        @JsonProperty("cocktail_kr")
        private final String cocktailKr;

        @JsonProperty("cocktail_en")
        private final String cocktailEn;

        @JsonProperty("max_alcohol")
        private final Integer maxAlcohol;

        @JsonProperty("min_alcohol")
        private final Integer minAlcohol;

        @JsonProperty("abv_band")
        private final String abvBand;

        @JsonProperty("taste_level")
        private final String tasteLevel;

        @JsonProperty("origin_text")
        private final String originText;

        @JsonProperty("image_url")
        private final String imageUrl;

        @JsonProperty("seasons")
        private final List<String> seasons;

        @JsonProperty("ingredients")
        private final List<IngredientDto> ingredients;

        @JsonProperty("tags")
        private final List<TagGroupDto> tags;

        public static AdminCocktailDetail from(Cocktail c) {
            String kor = c.getKorName() != null ? c.getKorName() : c.getCocktailKR();
            String eng = c.getEngName() != null ? c.getEngName() : c.getCocktailEN();
            String abv = c.getAbvBand() != null ? c.getAbvBand().name() : null;
            String taste = c.getTasteLevel() != null ? c.getTasteLevel().name() : null;

            List<String> seasonStrs = c.getSeasons() == null ? List.of()
                    : c.getSeasons().stream().filter(Objects::nonNull).map(Season::name).toList();

            List<IngredientDto> ingredients = new ArrayList<>();
            if (c.getIngredients() != null) {
                for (Ingredient i : c.getIngredients()) {
                    ingredients.add(new IngredientDto(i.getName(), i.getAmount()));
                }
            }

            // 태그 그룹핑 (CocktailMapper 와 동일 로직)
            Map<TagType, TagGroupDto> grouped = new LinkedHashMap<>();
            if (c.getTags() != null) {
                for (CocktailTag ct : c.getTags()) {
                    if (ct.getTag() == null) continue;
                    TagType type = ct.getTag().getType();
                    TagGroupDto g = grouped.computeIfAbsent(type, TagGroupDto::new);
                    g.getTags().add(new TagDto(ct.getTag().getId(), ct.getTag().getName()));
                }
            }
            List<TagGroupDto> tagGroups = new ArrayList<>(grouped.values());

            return new AdminCocktailDetail(c.getId(), kor, eng, c.getMaxAlcohol(), c.getMinAlcohol(),
                    abv, taste, c.getOriginText(), c.getImageUrl(),
                    seasonStrs, ingredients, tagGroups);
        }
    }

    @Getter
    @Setter
    public static class TagSaveReq {
        @JsonProperty("type")
        private String type;

        @JsonProperty("name")
        private String name;
    }
}
