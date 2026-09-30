package com.application.domain.main.service;

import com.application.domain.magazine.repository.MagazineArticleRepository;
import com.application.domain.magazine.service.AdminStoryService;
import com.application.domain.magazine.service.MagazineService;
import com.application.domain.main.dto.FeedItem;
import com.application.domain.main.dto.MainResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * #147 회귀 방지: 어드민 '뉴스' 메뉴로 올린 글이 홈 피드에 뜨고, 그 id 로 매거진 상세가 열려야 한다.
 * 예전엔 목록은 news 테이블, 상세는 magazine_article 에서 읽어 id 가 어긋났다.
 */
@SpringBootTest
@ActiveProfiles("test")
class MainFeedMagazineTest {

    @Autowired private AdminStoryService adminStoryService;
    @Autowired private MainFeedService mainFeedService;
    @Autowired private MagazineService magazineService;
    @Autowired private MagazineArticleRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("어드민으로 발행한 글은 홈 피드 카드의 id 그대로 상세가 열린다")
    void publishedStoryOpensFromHomeFeed() {
        long id = adminStoryService.create("Tom Collins", "존재하지 않는 남자", """
                ![잔](https://example.com/hero.jpg)
                *캡션*

                “Tom Collins가 네 욕하고 있던데?”

                ## 장난이 된 칵테일
                """, "TREND", "ONZ 매거진", LocalDateTime.now().minusMinutes(1));

        MainResponse main = mainFeedService.getMain(null, null, 20);
        FeedItem card = main.feed().stream()
                .filter(f -> "news".equals(f.type()))
                .findFirst().orElseThrow();

        assertThat(card.id()).isEqualTo(id);
        assertThat(card.categoryLabel()).isEqualTo("트렌드");
        assertThat(card.imageUrl()).isEqualTo("https://example.com/hero.jpg");

        assertThat(magazineService.detail(card.id())).hasValueSatisfying(d -> {
            assertThat(d.title()).isEqualTo("Tom Collins");
            assertThat(d.heroImage()).isEqualTo("https://example.com/hero.jpg");
        });
    }

    @Test
    @DisplayName("수정 화면은 마크다운으로 되돌려 보여주고, 저장하면 다시 블록이 된다")
    void editRoundTrip() {
        long id = adminStoryService.create("제목", null, "첫 문단 **굵게**", "BEGINNER", null, LocalDateTime.now());

        Map<String, Object> form = adminStoryService.get(id);
        assertThat(form.get("content")).isEqualTo("첫 문단 **굵게**\n");
        assertThat(form.get("category")).isEqualTo("BEGINNER");

        adminStoryService.update(id, "제목2", null, form.get("content") + "\n## 소제목\n", "BEGINNER", null,
                LocalDateTime.now());

        assertThat(repository.findById(id)).hasValueSatisfying(a -> {
            assertThat(a.getTitle()).isEqualTo("제목2");
            assertThat(a.getContent()).contains("\"type\":\"heading\"");
        });
    }
}
