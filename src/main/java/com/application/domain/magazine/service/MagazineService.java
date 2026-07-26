package com.application.domain.magazine.service;

import com.application.domain.magazine.dto.MagazineCard;
import com.application.domain.magazine.dto.MagazineDetail;
import com.application.domain.magazine.entity.MagazineArticle;
import com.application.domain.magazine.repository.MagazineArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MagazineService {

    private static final String PUBLISHED = "PUBLISHED";

    private final MagazineArticleRepository repository;

    /** 발행분 목록(최신순). category 지정 시 필터(대문자 정규화). */
    @Transactional(readOnly = true)
    public List<MagazineCard> list(String category) {
        List<MagazineArticle> rows = (category == null || category.isBlank() || "ALL".equalsIgnoreCase(category))
                ? repository.findByStatusOrderByPublishedAtDesc(PUBLISHED)
                : repository.findByStatusAndCategoryOrderByPublishedAtDesc(PUBLISHED, category.toUpperCase());
        return rows.stream().map(this::toCard).toList();
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
        return new MagazineCard(a.getId(), a.getSlug(), a.getTitle(), a.getDek(),
                a.getSubcategory(), a.getThumbnail(), a.getPublishedAt(),
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
