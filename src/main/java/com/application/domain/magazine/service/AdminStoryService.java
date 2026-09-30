package com.application.domain.magazine.service;

import com.application.domain.magazine.entity.MagazineArticle;
import com.application.domain.magazine.repository.MagazineArticleRepository;
import com.application.domain.news.dto.NewsCategory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * 어드민 '뉴스' 메뉴의 저장소. 마크다운 입력을 받아 magazine_article(STORY) 에 쓴다.
 *
 * 예전엔 news 테이블에 썼다. 그런데 앱은 홈 피드의 글을 눌렀을 때 /api/v2/magazine/{id} 로 상세를 찾는다 —
 * news 에만 있는 글은 "글을 불러오지 못했습니다"가 됐다(#147). 앱이 읽는 곳은 magazine_article 하나로 통일한다.
 * 뉴스 카테고리(TREND 등)는 subcategory 에 한국어 라벨로 담는다 — 앱이 칩에 그대로 보여주는 값이다.
 */
@Service
@RequiredArgsConstructor
public class AdminStoryService {

    static final String STORY = "STORY";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final MagazineArticleRepository repository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (MagazineArticle a : repository.findAll(Sort.by(Sort.Direction.DESC, "publishedAt", "id"))) {
            if (!STORY.equals(a.getCategory())) continue;
            Map<String, Object> row = new HashMap<>();
            row.put("id", a.getId());
            row.put("title", a.getTitle());
            row.put("category", NewsCategory.codeOf(a.getSubcategory()));
            row.put("view_count", a.getViewCount());
            row.put("published_at", a.getPublishedAt());
            rows.add(row);
        }
        return rows;
    }

    /** 수정 화면용. 템플릿이 쓰던 news 컬럼 키를 그대로 맞춘다. */
    @Transactional(readOnly = true)
    public Map<String, Object> get(long id) {
        MagazineArticle a = find(id);
        Map<String, Object> m = new HashMap<>();
        m.put("id", a.getId());
        m.put("title", a.getTitle());
        m.put("summary", a.getDek());
        m.put("category", NewsCategory.codeOf(a.getSubcategory()));
        m.put("source", a.getAuthorName());
        m.put("content", MarkdownBlocks.toMarkdown(a.getContent(), a.getHeroImage(), a.getImageCaption()));
        m.put("published_at", a.getPublishedAt());
        return m;
    }

    @Transactional
    public long create(String title, String summary, String markdown, String category,
                       String source, LocalDateTime publishedAt) {
        MagazineArticle a = new MagazineArticle();
        a.setSlug("story-" + UUID.randomUUID().toString().substring(0, 12));
        a.setCategory(STORY);
        a.setStatus("PUBLISHED");
        a.setRefs("{}");
        a.setCreatedAt(LocalDateTime.now());
        apply(a, title, summary, markdown, category, source, publishedAt);
        return repository.save(a).getId();
    }

    @Transactional
    public void update(long id, String title, String summary, String markdown, String category,
                       String source, LocalDateTime publishedAt) {
        MagazineArticle a = find(id);
        String before = a.getContent();
        apply(a, title, summary, markdown, category, source, publishedAt);
        a.setContent(keepSpecLinks(before, a.getContent()));
    }

    @Transactional
    public void delete(long id) {
        repository.delete(find(id));
    }

    private MagazineArticle find(long id) {
        return repository.findById(id)
                .filter(a -> STORY.equals(a.getCategory()))
                .orElseThrow(() -> new NoSuchElementException("글이 없습니다: " + id));
    }

    private static void apply(MagazineArticle a, String title, String summary, String markdown, String category,
                              String source, LocalDateTime publishedAt) {
        MarkdownBlocks.Result r = MarkdownBlocks.toBlocks(markdown);
        a.setTitle(title);
        a.setDek(blankToNull(summary));
        a.setSubcategory(NewsCategory.labelOf(category));
        a.setAuthorName(blankToNull(source));
        a.setContent(r.contentJson());
        a.setHeroImage(r.heroImage());
        a.setCoverImage(r.heroImage());
        a.setThumbnail(r.heroImage());
        a.setImageCaption(r.heroCaption());
        a.setWordCount(r.charCount());
        a.setReadingTimeMin(Math.max(1, Math.round(r.charCount() / 500f)));
        a.setPublishedAt(publishedAt);
        a.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * 스펙 카드의 칵테일 연결(cocktail_id·이름·사진)은 마크다운 표로 표현되지 않는다.
     * 수정 저장 때 이게 날아가지 않도록, 이전 본문의 스펙 카드에서 순서대로 옮겨 붙인다.
     */
    private static String keepSpecLinks(String beforeJson, String afterJson) {
        try {
            JsonNode before = MAPPER.readTree(beforeJson == null ? "[]" : beforeJson);
            ArrayNode after = (ArrayNode) MAPPER.readTree(afterJson);
            List<JsonNode> oldSpecs = new ArrayList<>();
            for (JsonNode b : before) {
                if ("cocktail_spec".equals(b.path("type").asText())) oldSpecs.add(b);
            }
            Iterator<JsonNode> old = oldSpecs.iterator();
            for (JsonNode b : after) {
                if (!"cocktail_spec".equals(b.path("type").asText()) || !old.hasNext()) continue;
                JsonNode prev = old.next();
                for (String key : List.of("name", "name_en", "image", "cocktail_id")) {
                    if (prev.has(key)) ((ObjectNode) b).set(key, prev.get(key));
                }
            }
            return after.toString();
        } catch (Exception e) {
            return afterJson;
        }
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.strip();
    }
}
