package com.application.domain.bar.service;

import com.application.domain.bar.dto.response.BarMenuDto;
import com.application.domain.bar.dto.response.BarMenuDto.CategoryDto;
import com.application.domain.bar.dto.response.BarMenuDto.MenuItemDto;
import com.application.domain.bar.entity.Bar;
import com.application.domain.bar.entity.BarMenuCategory;
import com.application.domain.bar.entity.BarMenuItem;
import com.application.domain.bar.repository.BarMenuCategoryRepository;
import com.application.domain.bar.repository.BarMenuItemRepository;
import com.application.domain.bar.trust.TrustLevel;
import com.application.domain.bar.trust.TrustLevelResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 게이트된 메뉴판 (T-12).
 *
 * 가격 노출 조건은 오직 하나: {@code trustLevel.atLeast(L2)}.
 * 가격을 마스킹하지 않고, 애초에 DTO 에 담지 않는다 → 응답 JSON 에 `price` 키가 없다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BarMenuService {

    /** 가격을 보려면 필요한 최소 신뢰등급. T-11 이후 bar.price_min_trust_level 컬럼으로 이관 예정. */
    private static final TrustLevel PRICE_MIN_TRUST = TrustLevel.L2;

    private final BarService barService;
    private final BarMenuCategoryRepository categoryRepository;
    private final BarMenuItemRepository itemRepository;
    private final TrustLevelResolver trustLevelResolver;

    public BarMenuDto getMenu(String slug, String sessionToken) {
        Bar bar = barService.getActiveBarOrThrow(slug);

        TrustLevel trust = trustLevelResolver.resolve(slug, sessionToken);
        boolean priceVisible = trust.atLeast(PRICE_MIN_TRUST);

        List<BarMenuCategory> categories = categoryRepository.findByBarIdOrderByPriorityAscIdAsc(bar.getId());

        Map<Long, List<BarMenuItem>> itemsByCategory = itemRepository
                .findByBarIdOrderByPriorityAscIdAsc(bar.getId()).stream()
                .filter(i -> i.getCategoryId() != null)
                .collect(Collectors.groupingBy(BarMenuItem::getCategoryId));

        List<CategoryDto> categoryDtos = categories.stream()
                .map(c -> CategoryDto.of(c, itemsByCategory
                        .getOrDefault(c.getId(), List.of()).stream()
                        .map(i -> MenuItemDto.of(i, priceVisible))
                        .toList()))
                .toList();

        return new BarMenuDto(priceVisible, trust.name(), categoryDtos);
    }
}
