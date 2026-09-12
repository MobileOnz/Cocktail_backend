package com.application.domain.cocktail.service;

import com.application.domain.cocktail.entity.Cocktail;
import com.application.domain.cocktail.entity.CocktailStep;
import com.application.domain.cocktail.repository.CocktailRepository;
import com.application.domain.cocktail.repository.CocktailStepRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 제조 단계 초안 생성기.
 *
 * <p>105종 중 12종(11%)만 손으로 쓴 단계를 갖고 있어 나머지는 "만드는 법"이 비어 있었다.
 * 칵테일 제조법은 재료 구성으로 기법이 거의 결정되므로(주스가 들어가면 셰이크, 전부 스피릿이면 스터,
 * 탄산이 들어가면 잔에서 빌드), 그 규칙으로 초안을 만든다.</p>
 *
 * <p><b>초안이다.</b> 사람이 쓴 단계(source=MANUAL)는 절대 건드리지 않고, 생성분은 AUTO 로 표시해
 * 앱과 어드민이 구분할 수 있게 한다. 재실행하면 AUTO 만 지우고 다시 쓴다.</p>
 *
 * <p>한계: 재료 문자열에서 기법을 추론하므로 원문 표기에 의존한다. 예외적인 제조법
 * (스로잉, 플로트, 스월링 등)은 규칙에 없어 가장 가까운 기법으로 떨어진다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CocktailStepGenerator {

    private static final String AUTO = "AUTO";

    /** 기법 판정 — 위에서부터 먼저 걸리는 것이 이긴다. */
    private enum Method { MUDDLE, CREAMY_SHAKE, SHAKE, BUILD, STIR }

    private static final List<String> CARBONATED =
            Arrays.asList("탄산", "소다", "토닉", "진저", "스파클링", "샴페인", "콜라", "스프라이트", "사이다", "프로세코", "카바");
    /**
     * 머들 대상은 <b>생재료</b>다. 처음엔 "민트"·"설탕"만 보고 판정했더니
     * '화이트 크렘 드 민트'(리큐르)와 '설탕 시럽'까지 걸려 105종 중 34종(32%)이 머들로 잘못 분류됐다.
     * → 생재료 표기(잎·웨지·각설탕)를 요구하고, 리큐르·시럽 표기는 아래에서 배제한다.
     */
    private static final List<String> MUDDLE_ITEMS =
            Arrays.asList("민트 잎", "민트잎", "라임 웨지", "라임 조각", "각설탕", "바질 잎", "오이", "생강 슬라이스");
    /** 이 단어가 들어 있으면 생재료가 아니라 리큐르·시럽이다. */
    private static final List<String> NOT_MUDDLE =
            Arrays.asList("시럽", "리큐르", "크렘", "코디얼", "주스", "퓨레", "비터");
    private static final List<String> CREAMY =
            Arrays.asList("크림", "우유", "계란", "에그", "달걀", "코코넛 밀크");
    private static final List<String> JUICY =
            Arrays.asList("주스", "레몬", "라임", "자몽", "오렌지", "파인애플", "퓨레", "시럽", "꿀");
    /** 가니시로 볼 만한 마지막 재료. 양 표기가 없고 이 단어로 끝나면 장식으로 본다. */
    private static final List<String> GARNISH_WORDS =
            Arrays.asList("슬라이스", "필", "제스트", "웨지", "체리", "올리브", "민트", "로즈마리", "가니시", "껍질", "잎");

    /** 잔별 얼음 안내. 없으면 기본 문구를 쓴다. */
    private static final Map<String, String> ICE_BY_GLASS = new LinkedHashMap<>() {{
        put("록스", "크고 단단한 얼음 한 덩이를");
        put("하이볼", "얼음을 잔 끝까지");
        put("콜린스", "얼음을 잔 끝까지");
        put("허리케인", "크러시드 아이스를 가득");
        put("티키 머그", "크러시드 아이스를 가득");
        put("줄렙 컵", "크러시드 아이스를 가득");
        put("구리 머그", "얼음을 가득");
        put("머그", "얼음을 가득");
    }};

    /** 잔을 미리 차갑게 두는 게 관건인 잔들(얼음 없이 내는 잔). */
    private static final List<String> CHILLED_GLASSES =
            Arrays.asList("쿠페", "마티니", "칵테일 글라스", "플루트", "와인 글라스", "마가리타");

    private final CocktailRepository cocktailRepository;
    private final CocktailStepRepository stepRepository;

    /**
     * 단계가 없는 칵테일에 초안을 채운다.
     *
     * @param overwriteAuto 이미 있는 AUTO 단계도 다시 만들지. false 면 비어 있는 것만 채운다.
     * @return 칵테일 이름 → 생성한 단계 수
     */
    @Transactional
    public Map<String, Integer> generateMissing(boolean overwriteAuto) {
        Map<String, Integer> result = new LinkedHashMap<>();

        for (Cocktail c : cocktailRepository.findAll()) {
            List<CocktailStep> existing = stepRepository.findByCocktailIdOrderByStepOrderAsc(c.getId());

            boolean hasManual = existing.stream().anyMatch(s -> !AUTO.equals(s.getSource()));
            if (hasManual) {
                continue; // 사람이 쓴 게 있으면 손대지 않는다.
            }
            if (!existing.isEmpty() && !overwriteAuto) {
                continue;
            }
            if (!existing.isEmpty()) {
                // (cocktail_id, step_order) 유니크 제약이 걸려 있다. 같은 트랜잭션에서
                // 삭제를 flush 하지 않으면 Hibernate 가 INSERT 를 먼저 내보내 중복키로 터진다.
                stepRepository.deleteAll(existing);
                stepRepository.flush();
            }

            List<String> parts = parseIngredients(c.getIngredientsText());
            if (parts.isEmpty()) {
                continue; // 재료를 모르면 만들 수 없다.
            }

            List<CocktailStep> steps = build(c, parts);
            if (steps.isEmpty()) {
                continue;
            }
            stepRepository.saveAll(steps);
            result.put(c.getKorName(), steps.size());
        }

        log.info("[STEP-GEN] {}종에 초안 생성", result.size());
        return result;
    }

    // ── 규칙 ──────────────────────────────────────────────────────────────

    private Method decide(List<String> parts, String glass) {
        String all = String.join(" ", parts);
        if (findMuddle(parts) != null) {
            return Method.MUDDLE;
        }
        if (containsAny(all, CREAMY)) {
            return Method.CREAMY_SHAKE;
        }
        if (containsAny(all, CARBONATED)) {
            return Method.BUILD;
        }
        if (containsAny(all, JUICY)) {
            return Method.SHAKE;
        }
        return Method.STIR;
    }

    /**
     * 실제로 으깰 생재료를 찾는다.
     *
     * 두 가지를 걸러낸다:
     *  - 리큐르·시럽 표기('화이트 크렘 드 민트', '설탕 시럽')
     *  - <b>수량이 없는 항목</b>. 마이타이의 '민트 잎'은 장식이고 모히또의 '민트 잎 6장'은 으깨는 재료다.
     *    수량 표기 유무가 둘을 가르는 유일한 신호다.
     */
    private String findMuddle(List<String> parts) {
        for (String p : parts) {
            if (!containsAny(p, MUDDLE_ITEMS) || containsAny(p, NOT_MUDDLE)) {
                continue;
            }
            if (!hasQuantity(p)) {
                continue; // 수량이 없으면 장식이다.
            }
            return p;
        }
        return null;
    }

    private boolean hasQuantity(String part) {
        return part.matches(".*\\d.*");
    }

    private List<CocktailStep> build(Cocktail c, List<String> parts) {
        String glass = nullSafe(c.getGlassType());
        Method m = decide(parts, glass);

        List<String> garnishes = extractGarnishes(parts);
        List<String> pour = new ArrayList<>(parts);
        pour.removeAll(garnishes);
        String carbonated = firstMatching(pour, CARBONATED);
        if (carbonated != null) {
            pour.remove(carbonated);
        }

        List<String> lines = new ArrayList<>();
        List<Integer> durations = new ArrayList<>();
        List<String> tips = new ArrayList<>();

        switch (m) {
            case MUDDLE -> {
                String muddle = findMuddle(pour);
                lines.add(glassLabel(glass) + "에 " + withObjectParticle(muddle != null ? muddle : "허브와 감미료") + " 넣고 가볍게 으깬다.");
                tips.add("세게 빻으면 쓴맛이 나온다. 향만 올라올 정도로.");
                durations.add(null);

                lines.add(withObjectParticle(join(pour, muddle)) + " 넣는다.");
                tips.add(null); durations.add(null);

                lines.add(iceLine(glass));
                tips.add(null); durations.add(null);

                if (carbonated != null) {
                    lines.add(carbonated + "로 채우고 한 번만 가볍게 젓는다.");
                    tips.add("많이 저으면 탄산이 날아간다.");
                    durations.add(null);
                }
            }
            case CREAMY_SHAKE -> {
                lines.add("셰이커에 " + withObjectParticle(join(pour, null)) + " 넣는다.");
                tips.add(null); durations.add(null);

                lines.add("얼음 없이 먼저 15초간 흔든다.");
                tips.add("드라이 셰이크. 크림과 계란이 충분히 섞여 거품이 산다.");
                durations.add(15);

                lines.add("얼음을 채우고 15초간 강하게 흔든다.");
                tips.add(null); durations.add(15);

                lines.add(strainLine(glass));
                tips.add(null); durations.add(null);
            }
            case SHAKE -> {
                lines.add("셰이커에 얼음을 가득 채운다.");
                tips.add(null); durations.add(null);

                lines.add(withObjectParticle(join(pour, null)) + " 넣는다.");
                tips.add(hasFreshCitrus(pour) ? "갓 짠 시트러스를 쓰면 향이 확 산다." : null);
                durations.add(null);

                lines.add("15초간 강하게 흔든다.");
                tips.add(null); durations.add(15);

                lines.add(strainLine(glass));
                tips.add(null); durations.add(null);
            }
            case BUILD -> {
                lines.add(iceLine(glass));
                tips.add(null); durations.add(null);

                lines.add(withObjectParticle(join(pour, null)) + " 붓는다.");
                tips.add(null); durations.add(null);

                if (carbonated != null) {
                    lines.add(withObjectParticle(carbonated) + " 잔 벽을 따라 천천히 채운다.");
                    tips.add("탄산은 마지막에, 한 번만 살짝 젓는다.");
                    durations.add(null);
                } else {
                    lines.add("가볍게 저어 섞는다.");
                    tips.add(null); durations.add(null);
                }
            }
            case STIR -> {
                lines.add("믹싱 글라스에 얼음을 채우고 " + withObjectParticle(join(pour, null)) + " 넣는다.");
                tips.add(null); durations.add(null);

                lines.add("20~30초간 충분히 젓는다.");
                tips.add("스터는 희석과 냉각을 같이 한다. 서두르지 않는다.");
                durations.add(25);

                lines.add(strainLine(glass));
                tips.add(null); durations.add(null);
            }
        }

        if (!garnishes.isEmpty()) {
            lines.add(String.join(", ", garnishes) + "로 장식한다.");
            tips.add(null); durations.add(null);
        }

        List<CocktailStep> out = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            out.add(CocktailStep.auto(c.getId(), i + 1, lines.get(i), tips.get(i), durations.get(i)));
        }
        return out;
    }

    // ── 문자열 도우미 ──────────────────────────────────────────────────────

    /** "다크 럼 45ml (1½ oz), 라임 주스 20ml" → ["다크 럼 45ml (1½ oz)", "라임 주스 20ml"] */
    private List<String> parseIngredients(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        // 재료 원문 일부에 <br> 이 섞여 있다(2종). 지시문에 태그가 그대로 새어 나오면 안 된다.
        String cleaned = text.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ");
        List<String> out = new ArrayList<>();
        for (String p : cleaned.split(",")) {
            String t = p.trim();
            // 괄호 안 oz 표기가 콤마로 잘려 나온 조각은 앞 항목에 도로 붙인다.
            if (t.isEmpty()) {
                continue;
            }
            if (!out.isEmpty() && t.startsWith(")")) {
                out.set(out.size() - 1, out.get(out.size() - 1) + ", " + t);
            } else {
                out.add(t);
            }
        }
        return out;
    }

    /**
     * 뒤에서부터 수량 없는 장식 항목을 연속으로 걷어낸다.
     * 마이타이는 '파인애플, 민트 잎, 라임 필' 세 개가 잇달아 장식이라 마지막 하나만 봐서는 부족하다.
     */
    private List<String> extractGarnishes(List<String> parts) {
        List<String> g = new ArrayList<>();
        for (int i = parts.size() - 1; i >= 1; i--) { // 최소 1개는 재료로 남긴다
            String p = parts.get(i);
            if (!hasQuantity(p) && containsAny(p, GARNISH_WORDS)) {
                g.add(0, p);
            } else {
                break;
            }
        }
        return g;
    }

    private String iceLine(String glass) {
        String ice = ICE_BY_GLASS.get(glass);
        if (ice != null) {
            return glassLabel(glass) + "에 " + ice + " 넣는다.";
        }
        return glassLabel(glass) + "에 얼음을 채운다.";
    }

    private String strainLine(String glass) {
        if (CHILLED_GLASSES.stream().anyMatch(g -> nullSafe(glass).contains(g))) {
            return "미리 차갑게 식힌 " + glassLabel(glass) + "에 걸러 따른다.";
        }
        return iceLine(glass) + " 그 위에 걸러 따른다.";
    }

    private String glassLabel(String glass) {
        String g = nullSafe(glass);
        if (g.isBlank()) {
            return "잔";
        }
        // "칵테일 글라스"처럼 이미 잔 이름이 붙어 있으면 그대로, 아니면 '잔'을 붙인다.
        return (g.contains("글라스") || g.contains("머그") || g.contains("컵")) ? g : g + " 잔";
    }

    /** 재료 목록을 사람이 읽는 문장으로. 3개를 넘으면 뒤는 묶는다. */
    private String join(List<String> parts, String exclude) {
        List<String> use = new ArrayList<>();
        for (String p : parts) {
            if (exclude != null && p.equals(exclude)) {
                continue;
            }
            use.add(p);
        }
        if (use.isEmpty()) {
            return "재료";
        }
        // 4가지 이상이면 "A, B, C 등 5가지" 로 줄여 쓰고 있었다. 레시피에서 재료를 생략하면
        // 읽는 사람이 나머지를 알 길이 없다(QA: "만드는 법이 등록 안 돼 있다"와 같은 체감).
        // 길어지더라도 전부 적는다.
        return String.join(", ", use);
    }

    private boolean hasFreshCitrus(List<String> parts) {
        String all = String.join(" ", parts);
        return all.contains("레몬") || all.contains("라임") || all.contains("자몽");
    }

    private String firstMatching(List<String> parts, List<String> needles) {
        for (String p : parts) {
            if (containsAny(p, needles)) {
                return p;
            }
        }
        return null;
    }

    private boolean containsAny(String haystack, List<String> needles) {
        String h = nullSafe(haystack);
        return needles.stream().anyMatch(h::contains);
    }

    /** 한글 받침에 따라 조사를 고른다. '민트 잎' + 를 → '민트 잎을'. */
    private String withObjectParticle(String word) {
        if (word == null || word.isEmpty()) {
            return "재료를";
        }
        char last = word.charAt(word.length() - 1);
        if (last < 0xAC00 || last > 0xD7A3) {
            return word + "를"; // 한글이 아니면(숫자·영문·괄호) 관례상 '를'
        }
        boolean hasFinalConsonant = (last - 0xAC00) % 28 != 0;
        return word + (hasFinalConsonant ? "을" : "를");
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
