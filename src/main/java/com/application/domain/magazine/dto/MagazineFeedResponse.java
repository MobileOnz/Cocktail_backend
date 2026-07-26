package com.application.domain.magazine.dto;

import java.util.List;

/** 매거진 목록 응답 봉투. 뉴스 피드와 동일 형태({items, nextCursor})로 앱 호환. */
public record MagazineFeedResponse(
        List<MagazineCard> items,
        String nextCursor
) {}
