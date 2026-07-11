package com.application.domain.bar.dto.response;

import com.application.domain.bar.entity.BarMenuCategory;
import com.application.domain.bar.entity.BarMenuItem;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 게이트된 메뉴판 (T-12).
 *
 * 가격은 **클라이언트에서 가리지 않는다. 서버가 필드를 넣지 않는다.**
 * priceVisible=false 이면 MenuItemDto.price 가 null 이 되고, @JsonInclude(NON_NULL) 에 의해
 * 응답 JSON 에서 `price` 키 자체가 사라진다. 프록시로 바디를 열어봐도 없다.
 * priceBand 는 L0 에게도 항상 제공한다(게이트 밖에서도 가치를 준다 — plan_FINAL D-22).
 */
public record BarMenuDto(
        boolean priceVisible,
        String trustLevel,
        List<CategoryDto> categories
) {

    public record CategoryDto(
            Long id,
            String nameKo,
            String nameEn,
            Double priority,
            List<MenuItemDto> items
    ) {
        public static CategoryDto of(BarMenuCategory c, List<MenuItemDto> items) {
            return new CategoryDto(c.getId(), c.getNameKo(), c.getNameEn(), c.getPriority(), items);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MenuItemDto(
            Long id,
            String name,
            String nameEn,
            String description,
            String notes,
            String menuImage,
            Boolean isAvailable,
            String priceBand,
            /** priceVisible=false 이면 null → 직렬화에서 키 자체가 제거된다. */
            String price,
            Long cocktailId
    ) {
        public static MenuItemDto of(BarMenuItem item, boolean priceVisible) {
            return new MenuItemDto(
                    item.getId(), item.getName(), item.getNameEn(),
                    item.getDescription(), item.getNotes(), item.getMenuImage(),
                    item.getIsAvailable(),
                    priceBand(item.getPriceAmount()),
                    priceVisible ? item.getPrice() : null,
                    item.getCocktailId()
            );
        }

        /**
         * 가격대. priceAmount 가 null 인 경우("시가")는 HIGH 로 본다 —
         * 시가 표기는 통상 고가 메뉴이며, LOW 로 오해하게 두는 것이 더 나쁘다.
         */
        static String priceBand(Integer amount) {
            if (amount == null) return "HIGH";
            if (amount < 15_000) return "LOW";
            if (amount < 22_000) return "MID";
            return "HIGH";
        }
    }
}
