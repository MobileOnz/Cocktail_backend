package com.application.domain.main.dto;

import java.util.List;

/**
 * 메인 첫 화면 합성 응답 (T-09). 왕복 1회로 hero + feed 제공.
 */
public record MainResponse(
        HeroDto hero,
        List<FeedItem> feed,
        String nextCursor
) {}
