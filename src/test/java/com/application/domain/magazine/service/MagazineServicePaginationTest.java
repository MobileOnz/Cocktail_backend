package com.application.domain.magazine.service;

import com.application.common.exception.custom.CustomApiException;
import com.application.domain.magazine.dto.MagazineCard;
import com.application.domain.magazine.dto.MagazineFeedResponse;
import com.application.domain.magazine.entity.MagazineArticle;
import com.application.domain.magazine.repository.MagazineArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 매거진 목록의 커서 페이지네이션.
 *
 * 커서가 미끄러지면 글이 조용히 사라지거나 중복된다 — 화면에서는 "가끔 안 보이는 글"로만
 * 나타나 재현이 어렵다. 그래서 경계(같은 발행시각, 마지막 페이지, 잘못된 커서)를 고정해 둔다.
 */
@SpringBootTest
@ActiveProfiles("test")
class MagazineServicePaginationTest {

    private static final int TOTAL = 25;
    private static final LocalDateTime TIE_AT = LocalDateTime.of(2026, 3, 1, 12, 0, 0);

    @Autowired
    private MagazineService magazineService;

    @Autowired
    private MagazineArticleRepository repository;

    @BeforeEach
    void seed() {
        repository.deleteAll();
        for (int i = 0; i < TOTAL; i++) {
            // 앞 5건은 발행시각을 일부러 같게 둔다 → id 2차 정렬 키가 없으면 여기서 커서가 미끄러진다.
            LocalDateTime publishedAt = (i < 5) ? TIE_AT : TIE_AT.minusDays(i);
            repository.save(article("글-" + i, i % 2 == 0 ? "STORY" : "GUIDE", "PUBLISHED", publishedAt));
        }
        // 미발행/발행시각 없음은 목록에 절대 나오면 안 된다.
        repository.save(article("초안", "STORY", "DRAFT", TIE_AT));
        repository.save(article("시각없음", "STORY", "PUBLISHED", null));
    }

    private MagazineArticle article(String title, String category, String status, LocalDateTime publishedAt) {
        MagazineArticle a = new MagazineArticle();
        a.setSlug(title + "-" + java.util.UUID.randomUUID());
        a.setTitle(title);
        a.setCategory(category);
        a.setStatus(status);
        a.setPublishedAt(publishedAt);
        a.setContent("[]");
        a.setRefs("[]");
        a.setViewCount(0);
        a.setIsFeatured(false);
        a.setCreatedAt(TIE_AT);
        a.setUpdatedAt(TIE_AT);
        return a;
    }

    /** 커서를 따라 끝까지 돈다. */
    private List<MagazineCard> drain(String category, int size) {
        List<MagazineCard> all = new ArrayList<>();
        String cursor = null;
        int guard = 0;
        do {
            MagazineFeedResponse page = magazineService.list(category, cursor, size);
            all.addAll(page.items());
            cursor = page.nextCursor();
            assertThat(++guard).as("커서가 끝나지 않고 계속 돎(무한 루프)").isLessThan(50);
        } while (cursor != null);
        return all;
    }

    @Test
    @DisplayName("페이지를 끝까지 넘기면 발행분 전체가 중복 없이 정확히 한 번씩 나온다")
    void drainsEveryArticleExactlyOnce() {
        List<MagazineCard> all = drain("ALL", 10);

        assertThat(all).hasSize(TOTAL);
        Set<Long> ids = new HashSet<>();
        all.forEach(c -> assertThat(ids.add(c.id())).as("중복된 글: %s", c.title()).isTrue());
    }

    @Test
    @DisplayName("발행시각이 같은 글이 여러 건이어도 건너뛰거나 겹치지 않는다")
    void handlesTiedPublishedAt() {
        // 경계가 동점 구간 한가운데(3)에 놓이도록 페이지 크기를 잡는다.
        List<MagazineCard> all = drain("ALL", 3);

        assertThat(all).hasSize(TOTAL);
        assertThat(all.stream().map(MagazineCard::id).distinct().count()).isEqualTo(TOTAL);
    }

    @Test
    @DisplayName("최신순으로 내려오고, 마지막 페이지의 nextCursor 는 null 이다")
    void ordersNewestFirstAndEndsWithNullCursor() {
        MagazineFeedResponse first = magazineService.list("ALL", null, 10);
        assertThat(first.items()).hasSize(10);
        assertThat(first.nextCursor()).isNotNull();

        List<MagazineCard> all = drain("ALL", 10);
        for (int i = 1; i < all.size(); i++) {
            assertThat(all.get(i).publishedAt())
                    .as("최신순이 깨짐: %s 뒤에 %s", all.get(i - 1).title(), all.get(i).title())
                    .isBeforeOrEqualTo(all.get(i - 1).publishedAt());
        }

        // 전체보다 큰 페이지를 요청하면 한 번에 끝나고 커서가 없다.
        MagazineFeedResponse whole = magazineService.list("ALL", null, 50);
        assertThat(whole.items()).hasSize(TOTAL);
        assertThat(whole.nextCursor()).isNull();
    }

    @Test
    @DisplayName("DRAFT 와 발행시각 없는 글은 어느 페이지에도 나오지 않는다")
    void excludesDraftAndUnpublished() {
        List<String> titles = drain("ALL", 7).stream().map(MagazineCard::title).toList();
        assertThat(titles).doesNotContain("초안", "시각없음");
    }

    @Test
    @DisplayName("카테고리 필터는 페이지를 넘겨도 유지된다")
    void keepsCategoryFilterAcrossPages() {
        List<MagazineCard> story = drain("STORY", 4);

        assertThat(story).isNotEmpty();
        assertThat(story).allSatisfy(c -> assertThat(c.category()).isEqualTo("STORY"));
        assertThat(story).hasSize(13); // 0,2,4,...,24
    }

    @Test
    @DisplayName("잘못된 커서는 조용히 첫 페이지로 되돌지 않고 400 으로 거절한다")
    void rejectsMalformedCursor() {
        // 조용히 첫 페이지를 돌려주면 앱이 같은 페이지를 무한히 다시 받는다.
        assertThatThrownBy(() -> magazineService.list("ALL", "이건커서가아니다", 10))
                .isInstanceOf(CustomApiException.class);
        assertThatThrownBy(() -> magazineService.list("ALL", "abc_def", 10))
                .isInstanceOf(CustomApiException.class);
    }

    @Test
    @DisplayName("size 는 1~50 으로 잘린다")
    void clampsPageSize() {
        assertThat(magazineService.list("ALL", null, 0).items()).hasSize(1);
        assertThat(magazineService.list("ALL", null, -3).items()).hasSize(1);
        assertThat(magazineService.list("ALL", null, 999).items()).hasSize(TOTAL);
        assertThat(magazineService.list("ALL", null, null).items()).hasSize(20); // 기본값
    }
}
