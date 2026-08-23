package com.application.domain.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 관리자 칵테일 조회 (읽기 전용).
 *
 * <p>어드민 칵테일 관리 그리드(AG Grid)가 호출하는 {@code GET /admin/search/cocktail} 의 데이터 소스.
 * s1 소유의 {@code domain/cocktail} 엔티티/서비스를 <b>수정하지 않기 위해</b> 이미 검증된
 * {@code cocktail} 테이블을 JdbcTemplate 네이티브 SQL로 <b>읽기만</b> 한다.
 * (T-18/T-19의 AdminContentService·AdminBarService 와 동일 전략)</p>
 */
@Service
@RequiredArgsConstructor
public class AdminCocktailService {

    private final JdbcTemplate jdbc;

    /**
     * 이름(kor/eng) 부분일치 필터 + 페이징 조회.
     * AG Grid 컬럼 필드(cocktail_kr / cocktail_en / abv_band / taste_level / createdAt)에 맞춰 별칭을 준다.
     *
     * @param cocktailName null/빈문자면 전체
     * @param page 0-base 페이지
     * @param size 페이지 크기(1~200 clamp)
     */
    public List<Map<String, Object>> search(String cocktailName, int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 200));
        int safePage = Math.max(0, page);
        int offset = safePage * safeSize;

        StringBuilder sql = new StringBuilder(
                "SELECT id, kor_name AS cocktail_kr, eng_name AS cocktail_en, " +
                "       abv_band, taste_level, created_at AS \"createdAt\" " +
                "FROM cocktail ");
        List<Object> args = new ArrayList<>();

        if (cocktailName != null && !cocktailName.isBlank()) {
            sql.append("WHERE kor_name ILIKE ? OR eng_name ILIKE ? ");
            String like = "%" + cocktailName.trim() + "%";
            args.add(like);
            args.add(like);
        }
        sql.append("ORDER BY id ASC LIMIT ? OFFSET ?");
        args.add(safeSize);
        args.add(offset);

        return jdbc.queryForList(sql.toString(), args.toArray());
    }
}
