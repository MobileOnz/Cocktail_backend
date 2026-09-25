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

    /**
     * UI 라벨 '기타'로 도달해야 하는 DB 베이스 값 목록.
     *
     * <p>'기타'는 다른 라벨과 달리 라벨 자신으로 매칭하지 않고 이 목록을 OR 로 순회한다
     * (CocktailRepositoryImpl 참고). 그래서 DB 값이 문자 그대로 '기타'인 행도
     * 여기에 있어야 걸린다 — 없으면 어느 라벨로도 도달할 수 없다.</p>
     *
     * <p>소주·사케·맥주는 신규 데이터와 함께 들어온 베이스인데 앱에 대응 라벨이 없다.
     * 라벨을 새로 파려면 앱 배포가 필요하므로 우선 '기타' 아래로 모은다.</p>
     *
     * <p>[주의] '무알코올'은 여기 넣지 않는다. 앱이 독립 라벨로 노출하며,
     * {@link #aliasesOf(String)} 이 등록 없는 라벨은 라벨 자신을 반환하므로 그대로 동작한다.
     * 여기 추가하면 같은 칵테일이 '무알코올'과 '기타' 양쪽에 중복 노출된다.</p>
     */
    public static final List<String> ETC_BASES = List.of(
            "기타",
            "셰리",
            "그라파",
            "앙고스투라 비터스",
            "카샤사",
            "카샤샤",
            "메스칼",
            "피스코",
            "메즈칼",
            "코냑",
            "소주",
            "사케",
            "맥주"
    );
}
