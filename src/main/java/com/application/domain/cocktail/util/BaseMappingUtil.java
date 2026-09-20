package com.application.domain.cocktail.util;

import java.util.List;
import java.util.Map;

public class BaseMappingUtil {

    /**
     * UI 라벨 → DB 에 실제로 들어 있는 표기들.
     *
     * <p>앱 필터는 '데킬라'를 보내는데 DB 는 전부 '테킬라'다. 서버가 contains 부분일치로 찾으므로
     * 글자가 다르면 한 건도 안 걸린다 — 프로덕션에서 '데킬라' 필터가 항상 0건이었다.
     * 표준 표기는 '테킬라'라 데이터가 맞고 앱 라벨이 틀렸지만, 앱은 이미 배포돼 있으므로
     * 서버가 양쪽을 다 받아준다.</p>
     *
     * <p>표기가 갈린 것만 등록한다. 여기 없는 라벨은 기존대로 라벨 그대로 매칭한다.
     * (메스칼·메즈칼은 아래 ETC_BASES 에서 '기타'로 이미 도달 가능하므로 여기 넣지 않는다 —
     *  넣으면 같은 칵테일이 '데킬라'와 '기타' 양쪽에 걸린다.)</p>
     */
    public static final Map<String, List<String>> BASE_ALIASES = Map.of(
            "데킬라", List.of("데킬라", "테킬라")
    );

    /** UI 라벨에 대응하는 DB 표기들. 등록된 별칭이 없으면 라벨 자신만 반환한다. */
    public static List<String> aliasesOf(String uiLabel) {
        return BASE_ALIASES.getOrDefault(uiLabel, List.of(uiLabel));
    }

    // UI에 '기타'로 표시될 베이스들의 실제 DB 값 목록
    public static final List<String> ETC_BASES = List.of(
            "셰리",
            "그라파",
            "앙고스투라 비터스",
            "카샤사",
            "카샤샤",
            "메스칼",
            "피스코",
            "메즈칼",
            "코냑"
    );
}
