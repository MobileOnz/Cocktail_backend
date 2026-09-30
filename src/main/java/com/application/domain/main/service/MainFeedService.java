package com.application.domain.main.service;

import com.application.domain.cocktail.dto.response.RecommendationResult;
import com.application.domain.cocktail.service.RecommendationService;
import com.application.domain.main.dto.FeedItem;
import com.application.domain.main.dto.HeroDto;
import com.application.domain.main.dto.MainResponse;
import com.application.domain.member.entity.Member;
import com.application.domain.member.repository.MemberRepository;
import com.application.domain.magazine.dto.MagazineCard;
import com.application.domain.magazine.dto.MagazineFeedResponse;
import com.application.domain.magazine.service.MagazineService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 메인 첫 화면 합성 (T-09). hero(개인화 추천) + feed(뉴스/가이드 인터리브)를 왕복 1회로 제공.
 * 인터리브 규칙: 뉴스 {@value #NEWS_PER_GUIDE}개마다 가이드 카드 1개 삽입.
 *
 * '뉴스' 카드는 magazine_article(STORY) 에서 읽는다. 앱이 카드를 누르면 /api/v2/magazine/{id} 로 상세를 찾기 때문에,
 * 목록과 상세가 같은 테이블을 봐야 id 가 맞는다. news 테이블에서 읽던 시절엔 어드민으로 새로 올린 글이
 * 목록엔 뜨고 상세는 "글을 불러오지 못했습니다"가 됐다(#147).
 */
@Service
@RequiredArgsConstructor
public class MainFeedService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 40;
    private static final int NEWS_PER_GUIDE = 4;
    private static final String STORY = "STORY";

    private final RecommendationService recommendationService;
    private final MagazineService magazineService;
    private final MemberRepository memberRepository;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public MainResponse getMain(String credentialId, String cursor, Integer size) {
        // ── hero ──
        Member member = (credentialId == null) ? null : memberRepository.findByCredentialId(credentialId);
        RecommendationResult rec = (member == null)
                ? recommendationService.recommendForAnonymous()
                : recommendationService.recommendForMember(member.getId());
        HeroDto hero = HeroDto.from(rec);

        // ── feed: 뉴스 페이지 + 가이드 인터리브 ──
        int limit = (size == null || size <= 0) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        MagazineFeedResponse stories = magazineService.list(STORY, cursor, limit);

        List<Object[]> guides = guideCards();
        List<FeedItem> feed = new ArrayList<>();
        int guideIdx = 0;
        int newsSinceGuide = 0;
        for (MagazineCard c : stories.items()) {
            feed.add(FeedItem.news(c));
            newsSinceGuide++;
            if (newsSinceGuide == NEWS_PER_GUIDE && !guides.isEmpty()) {
                Object[] g = guides.get(guideIdx % guides.size());
                feed.add(FeedItem.guide(((Number) g[0]).intValue(), str(g[1]), str(g[2])));
                guideIdx++;
                newsSinceGuide = 0;
            }
        }

        return new MainResponse(hero, feed, stories.nextCursor());
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> guideCards() {
        return em.createNativeQuery(
                "SELECT part, title, image_url FROM guide ORDER BY part").getResultList();
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}
