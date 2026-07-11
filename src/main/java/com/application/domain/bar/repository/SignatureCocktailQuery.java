package com.application.domain.bar.repository;

import com.application.domain.bar.dto.response.BarDetailDto.SignatureCocktailDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

/**
 * 바의 시그니처 칵테일(= 메뉴에 등록되어 있고 레시피북 cocktail 과 연결된 항목) 조회.
 *
 * domain/cocktail 은 다른 워크스트림(s1) 소유이므로 Cocktail 엔티티에 연관관계를 걸지 않는다.
 * 대신 cocktail 테이블을 네이티브 프로젝션으로만 읽는다. 결합도 0.
 */
@Repository
public class SignatureCocktailQuery {

    @PersistenceContext
    private EntityManager em;

    private static final String SQL = """
            SELECT c.id, c.kor_name, c.image_url
              FROM bar_menu_item mi
              JOIN cocktail c ON c.id = mi.cocktail_id
             WHERE mi.bar_id = :barId
               AND mi.cocktail_id IS NOT NULL
               AND mi.is_available = true
             ORDER BY mi.priority ASC, mi.id ASC
             LIMIT 5
            """;

    @SuppressWarnings("unchecked")
    public List<SignatureCocktailDto> findByBarId(Long barId) {
        if (barId == null) return Collections.emptyList();
        List<Object[]> rows = em.createNativeQuery(SQL)
                .setParameter("barId", barId)
                .getResultList();

        return rows.stream()
                .map(r -> new SignatureCocktailDto(
                        ((Number) r[0]).longValue(),
                        (String) r[1],
                        (String) r[2]))
                .toList();
    }
}
