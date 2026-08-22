package com.application.domain.magazine.service;

import com.application.common.exception.custom.CustomApiException;
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

    /** 커서는 정렬 키(publishedAt, id) 를 그대로 담는다. 밀리초_id 형태. */
    private record Cursor(LocalDateTime at, Long id) {}

    private static String encodeCursor(MagazineArticle a) {
        return a.getPublishedAt().toInstant(ZoneOffset.UTC).toEpochMilli() + "_" + a.getId();
    }

    private static Cursor decodeCursor(String cursor) {
        // 앱이 커서를 임의로 만들어 보내면 여기서 걸러 400 으로 돌려준다.
        // 조용히 첫 페이지로 되돌리면 앱이 같은 페이지를 무한히 다시 받는다.
        String[] parts = cursor.split("_");
        if (parts.length != 2) {
            throw new CustomApiException("잘못된 커서입니다.");
        }
        try {
            LocalDateTime at = LocalDateTime.ofEpochSecond(
                    Long.parseLong(parts[0]) / 1000,
                    (int) (Long.parseLong(parts[0]) % 1000) * 1_000_000,
                    ZoneOffset.UTC);
            return new Cursor(at, Long.parseLong(parts[1]));
        } catch (NumberFormatException e) {
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
                a.getDek(),
                a.getCategory(),
                a.getSubcategory(),
                a.getThumbnail(),
                a.getAuthorName(),
                null,
                a.getPublishedAt(),
                a.getViewCount(),
                repository.findTagNames(a.getId()));
    }

    private MagazineDetail toDetail(MagazineArticle a) {
        return new MagazineDetail(a.getId(), a.getSlug(), a.getTitle(), a.getTitleLines(),
                a.getDek(), a.getCategory(), a.getSubcategory(), a.getHeroImage(), a.getCoverImage(),
                a.getThumbnail(), a.getImageCaption(), a.getAuthorName(), a.getContent(), a.getRefs(),
                a.getReadingTimeMin(), a.getViewCount(), a.getPublishedAt(),
                repository.findTagNames(a.getId()));
    }
}
