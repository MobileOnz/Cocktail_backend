package com.application.domain.magazine.service;

import com.application.common.exception.custom.CustomApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.application.domain.magazine.dto.MagazineCard;
import com.application.domain.magazine.dto.MagazineDetail;
import com.application.domain.magazine.dto.MagazineFeedResponse;
import com.application.domain.magazine.entity.MagazineArticle;
import com.application.domain.magazine.repository.MagazineArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MagazineService {

    private static final String PUBLISHED = "PUBLISHED";

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final MagazineArticleRepository repository;

    /** content(JSONB 원본 문자열) 에서 첫 문단을 꺼낼 때만 쓴다. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 카드 요약 폴백 길이. 앱 카드가 2줄을 넘기지 않는 선. */
    private static final int SUMMARY_MAX = 80;

    /**
     * 발행분 목록(최신순). category 지정 시 필터(대문자 정규화).
     *
     * 예전엔 전량을 한 번에 내려줬다. 글이 늘수록 첫 화면이 느려지고, 카드마다 태그를 따로 읽는
     * 구조라 쿼리 수도 글 수에 비례해 늘었다 → 커서 페이지네이션으로 한 페이지 분량만 읽는다.
     *
     * @param cursor 이전 응답의 nextCursor. null 이면 첫 페이지.
     * @param size   페이지 크기(1~50, 기본 20).
     */
    @Transactional(readOnly = true)
    public MagazineFeedResponse list(String category, String cursor, Integer size) {
        int limit = (size == null) ? DEFAULT_SIZE : Math.min(Math.max(size, 1), MAX_SIZE);
        String cat = (category == null || category.isBlank() || "ALL".equalsIgnoreCase(category))
                ? null
                : category.toUpperCase();

        // limit + 1 을 읽어 "다음 페이지가 있는지"를 별도 count 쿼리 없이 판단한다.
        Pageable page = PageRequest.of(0, limit + 1);
        List<MagazineArticle> rows = (cursor == null || cursor.isBlank())
                ? repository.findFirstPage(PUBLISHED, cat, page)
                : nextPage(cat, cursor, page);

        boolean hasMore = rows.size() > limit;
        List<MagazineArticle> pageRows = hasMore ? rows.subList(0, limit) : rows;
        String nextCursor = hasMore ? encodeCursor(pageRows.get(pageRows.size() - 1)) : null;

        return new MagazineFeedResponse(pageRows.stream().map(this::toCard).toList(), nextCursor);
    }

    private List<MagazineArticle> nextPage(String category, String cursor, Pageable page) {
        Cursor c = decodeCursor(cursor);
        return repository.findNextPage(PUBLISHED, category, c.at(), c.id(), page);
    }

    /**
     * 커서는 정렬 키(publishedAt, id) 를 그대로 담는다. "초_나노_id" 형태.
     *
     * 처음엔 밀리초로 담았는데, published_at 은 마이크로초까지 저장된다(예: 07:26:32.412269).
     * 밀리초로 자르면 커서가 .412 가 되어 실제 값 .412269 보다 앞서고,
     * 다음 페이지 조건 `publishedAt < .412` 도 `publishedAt = .412` 도 그 행들을 못 잡는다.
     * 같은 시각에 발행된 나머지가 통째로 건너뛰어졌다(GUIDE 12건 중 5건만 도달).
     * → 초와 나노를 따로 담아 무손실로 왕복시킨다.
     */
    private record Cursor(LocalDateTime at, Long id) {}

    private static String encodeCursor(MagazineArticle a) {
        Instant at = a.getPublishedAt().toInstant(ZoneOffset.UTC);
        return at.getEpochSecond() + "_" + at.getNano() + "_" + a.getId();
    }

    private static Cursor decodeCursor(String cursor) {
        // 앱이 커서를 임의로 만들어 보내면 여기서 걸러 400 으로 돌려준다.
        // 조용히 첫 페이지로 되돌리면 앱이 같은 페이지를 무한히 다시 받는다.
        String[] parts = cursor.split("_");
        if (parts.length != 3) {
            throw new CustomApiException("잘못된 커서입니다.");
        }
        try {
            LocalDateTime at = LocalDateTime.ofEpochSecond(
                    Long.parseLong(parts[0]),
                    Integer.parseInt(parts[1]),
                    ZoneOffset.UTC);
            return new Cursor(at, Long.parseLong(parts[2]));
        } catch (NumberFormatException | java.time.DateTimeException e) {
            // 나노 자리가 범위를 벗어나면 DateTimeException 이 난다 — 이것도 잘못된 커서다.
            throw new CustomApiException("잘못된 커서입니다.");
        }
    }

    @Transactional(readOnly = true)
    public Optional<MagazineDetail> detail(Long id) {
        return repository.findById(id).map(this::toDetail);
    }

    /** 조회수 +1. 없으면 false. */
    @Transactional
    public boolean markRead(Long id) {
        return repository.incrementViewCount(id) > 0;
    }

    private MagazineCard toCard(MagazineArticle a) {
        return new MagazineCard(
                a.getId(),
                a.getTitle(),
                summaryOf(a),
                a.getCategory(),
                a.getSubcategory(),
                a.getThumbnail(),
                a.getAuthorName(),
                null,
                a.getPublishedAt(),
                a.getViewCount(),
                repository.findTagNames(a.getId()));
    }

    /**
     * 목록 카드에 쓸 요약.
     *
     * dek 이 있으면 그대로 쓴다. 가이드에서 넘어온 글들은 dek 이 비어 있는데(V12 마이그레이션이
     * 채우지 않았다), 그러면 카드에 제목 한 줄만 남아 25장이 서로 구별되지 않는다.
     * 그 경우 본문 첫 문단 앞부분을 잘라 쓴다 — 편집자가 쓴 리드문만은 못해도,
     * 빈 카드보다는 글을 고르는 데 도움이 된다. dek 이 채워지면 자동으로 그쪽이 이긴다.
     */
    private String summaryOf(MagazineArticle a) {
        if (a.getDek() != null && !a.getDek().isBlank()) {
            return a.getDek();
        }
        return firstParagraphSnippet(a.getContent());
    }

    private static String firstParagraphSnippet(String contentJson) {
        if (contentJson == null || contentJson.isBlank()) {
            return null;
        }
        try {
            JsonNode blocks = MAPPER.readTree(contentJson);
            if (!blocks.isArray()) {
                return null;
            }
            for (JsonNode block : blocks) {
                if (!block.isObject()) {
                    continue;
                }
                if (!"paragraph".equals(block.path("type").asText())) {
                    continue;
                }
                String text = block.path("text").asText("").trim();
                if (text.isEmpty()) {
                    continue;
                }
                return truncate(text);
            }
            return null;
        } catch (Exception e) {
            // 목록 조회가 본문 파싱 때문에 실패하면 안 된다. 요약만 포기한다.
            return null;
        }
    }

    /** 낱말 중간에서 자르지 않는다. */
    private static String truncate(String text) {
        if (text.length() <= SUMMARY_MAX) {
            return text;
        }
        String cut = text.substring(0, SUMMARY_MAX);
        int lastSpace = cut.lastIndexOf(' ');
        if (lastSpace > SUMMARY_MAX / 2) {
            cut = cut.substring(0, lastSpace);
        }
        return cut.stripTrailing() + "…";
    }

    private MagazineDetail toDetail(MagazineArticle a) {
        return new MagazineDetail(a.getId(), a.getSlug(), a.getTitle(), a.getTitleLines(),
                a.getDek(), a.getCategory(), a.getSubcategory(), a.getHeroImage(), a.getCoverImage(),
                a.getThumbnail(), a.getImageCaption(), a.getAuthorName(), a.getContent(), a.getRefs(),
                a.getReadingTimeMin(), a.getViewCount(), a.getPublishedAt(),
                repository.findTagNames(a.getId()));
    }
}
