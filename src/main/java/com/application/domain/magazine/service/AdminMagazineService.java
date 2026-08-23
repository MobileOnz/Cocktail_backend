package com.application.domain.magazine.service;

import com.application.domain.cocktail.entity.Tag;
import com.application.domain.cocktail.enums.TagType;
import com.application.domain.cocktail.repository.TagRepository;
import com.application.domain.magazine.entity.MagazineArticle;
import com.application.domain.magazine.repository.MagazineArticleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * 어드민 매거진 JSON 주입: 붙여넣은 JSON 을 검증 후 slug 기준 upsert.
 * content/refs/title_lines 는 파싱하지 않고 원본 JSON 문자열로 저장(스키마 유연성).
 */
@Service
@RequiredArgsConstructor
public class AdminMagazineService {

    private static final Set<String> ALLOWED_TAG_TYPES =
            Set.of("FLAVOR", "MOOD", "BASE", "GLASS");
    private static final EnumSet<TagType> ALL = EnumSet.allOf(TagType.class);

    private final MagazineArticleRepository repository;
    private final TagRepository tagRepository;
    private final ObjectMapper objectMapper;

    /** 붙여넣은 JSON 으로 매거진 글을 생성/갱신하고 저장된 글을 반환. */
    @Transactional
    public MagazineArticle upsertFromJson(String rawJson, boolean publish) {
        JsonNode root;
        try {
            root = objectMapper.readTree(rawJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON 파싱 실패: " + e.getMessage());
        }

        String slug = text(root, "slug");
        String title = text(root, "title");
        JsonNode content = root.get("content");
        String category = text(root, "category");
        if (slug == null || slug.isBlank()) throw new IllegalArgumentException("필수 필드 'slug' 누락");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("필수 필드 'title' 누락");
        if (category == null || category.isBlank()) throw new IllegalArgumentException("필수 필드 'category' 누락");
        if (content == null || !content.isArray()) throw new IllegalArgumentException("필수 필드 'content'(배열) 누락");

        MagazineArticle a = repository.findBySlug(slug).orElseGet(MagazineArticle::new);
        LocalDateTime now = LocalDateTime.now();
        if (a.getId() == null) {
            a.setCreatedAt(now);
            a.setViewCount(0);
        }
        a.setSlug(slug);
        a.setTitle(title);
        a.setCategory(category.toUpperCase());
        a.setSubcategory(text(root, "subcategory"));
        a.setDek(text(root, "dek"));
        a.setTitleLines(rawOrNull(root.get("title_lines")));
        a.setCoverImage(text(root, "cover_image"));
        a.setHeroImage(text(root, "hero_image"));
        a.setThumbnail(text(root, "thumbnail"));
        a.setImageCaption(text(root, "image_caption"));
        JsonNode author = root.get("author");
        a.setAuthorName(author != null ? text(author, "name") : null);
        a.setAuthorBio(author != null ? text(author, "bio") : null);
        a.setAuthorAvatar(author != null ? text(author, "avatar") : null);
        a.setContent(content.toString());
        a.setRefs(root.has("refs") && !root.get("refs").isNull() ? root.get("refs").toString() : "{}");

        int wordCount = countWords(content);
        a.setWordCount(wordCount);
        a.setReadingTimeMin(Math.max(1, (int) Math.ceil(wordCount / 400.0)));

        Boolean featured = root.has("is_featured") ? root.get("is_featured").asBoolean(false) : Boolean.FALSE;
        a.setIsFeatured(featured);

        if (publish) {
            a.setStatus("PUBLISHED");
            a.setPublishedAt(parseDateTime(text(root, "published_at"), now));
        } else {
            a.setStatus("DRAFT");
            if (a.getPublishedAt() == null) a.setPublishedAt(parseDateTime(text(root, "published_at"), null));
        }
        a.setUpdatedAt(now);

        MagazineArticle saved = repository.save(a);
        syncTags(saved.getId(), root.get("tags"));
        return saved;
    }

    private void syncTags(Long articleId, JsonNode tags) {
        repository.clearTags(articleId);
        if (tags == null || !tags.isArray()) return;
        for (JsonNode t : tags) {
            String type = text(t, "type");
            String label = text(t, "label");
            if (label == null || label.isBlank()) label = text(t, "value");
            if (type == null || label == null || label.isBlank()) continue;
            String upper = type.toUpperCase();
            if (!ALLOWED_TAG_TYPES.contains(upper)) continue; // CHECK 제약: 4종만
            TagType tagType = TagType.valueOf(upper);
            final String name = label;
            Tag tag = tagRepository.findAll().stream()
                    .filter(x -> name.equals(x.getName()))
                    .findFirst()
                    .orElseGet(() -> tagRepository.save(new Tag(tagType, name)));
            repository.addTag(articleId, tag.getId());
        }
    }

    private int countWords(JsonNode content) {
        int total = 0;
        for (JsonNode block : content) {
            JsonNode txt = block.get("text");
            if (txt != null && txt.isTextual()) total += txt.asText().length();
        }
        return total;
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    private static String rawOrNull(JsonNode node) {
        return (node == null || node.isNull()) ? null : node.toString();
    }

    private static LocalDateTime parseDateTime(String s, LocalDateTime fallback) {
        if (s == null || s.isBlank()) return fallback;
        try {
            return OffsetDateTime.parse(s).toLocalDateTime();
        } catch (Exception ignored) { }
        try {
            return LocalDateTime.parse(s);
        } catch (Exception ignored) { }
        return fallback;
    }
}
