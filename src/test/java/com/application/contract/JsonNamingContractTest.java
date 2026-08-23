package com.application.contract;

import com.application.common.response.ResponseDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-05(b) — JSON 계약 고정.
 *
 * <p>경계 규칙: <b>봉투는 {@code code/msg/data} 유지, data 내부 키는 camelCase.</b>
 * (DB/Prisma 는 snake_case 지만 와이어 JSON 은 camelCase — 앱스토어 앱이 이미 그 키를 읽는다.)</p>
 *
 * <p>두 가지를 고정한다:
 * <ol>
 *   <li>대표 응답을 직렬화해 최상위 키가 정확히 code/msg/data 이고 data 내부가 재귀적으로 camelCase.</li>
 *   <li>회귀 방지: application*.yml 에 snake_case naming strategy 가 <b>설정되지 않았는지</b> 확인.
 *       (누군가 {@code spring.jackson.property-naming-strategy=SNAKE_CASE} 를 켜면 전체 키가
 *       snake_case 로 뒤집혀 앱이 깨진다 — 그 회귀를 여기서 잡는다.)</li>
 * </ol>
 * </p>
 */
class JsonNamingContractTest {

    private final ObjectMapper mapper = new ObjectMapper();

    // camelCase: 소문자로 시작, 밑줄 없음, 전부 대문자(SNAKE 상수형)도 아님
    private static final Pattern CAMEL = Pattern.compile("^[a-z][a-zA-Z0-9]*$");

    /** 대표 응답 data 페이로드 — 여러 도메인의 camelCase 필드를 모사(중첩 포함). */
    record SampleBar(long id, String slug, String nameKo, boolean isVisited,
                     String heroImage, List<SampleCocktail> signatureCocktails) {}
    record SampleCocktail(long id, String korName, String imageUrl, int recommendCount) {}

    @Test
    @DisplayName("봉투는 code/msg/data, data 내부 키는 전부 camelCase")
    void envelopeAndCamelCase() throws Exception {
        SampleBar payload = new SampleBar(1L, "alice-cheongdam", "앨리스 청담", true,
                "https://x/y.webp",
                List.of(new SampleCocktail(7L, "진토닉", "https://x/g.webp", 42)));

        JsonNode root = mapper.readTree(
                mapper.writeValueAsString(ResponseDto.onSuccess("성공", payload)));

        // (1) 봉투 최상위 키 == code, msg, data
        List<String> topKeys = new ArrayList<>();
        root.fieldNames().forEachRemaining(topKeys::add);
        assertThat(topKeys).containsExactlyInAnyOrder("code", "msg", "data");

        // (2) data 내부의 모든 객체 키가 camelCase (snake_case/대문자 상수형 0건)
        List<String> violations = new ArrayList<>();
        collectNonCamelKeys(root.get("data"), "data", violations);
        assertThat(violations)
                .as("snake_case 또는 non-camelCase 키 발견(와이어 계약 위반): %s", violations)
                .isEmpty();
    }

    @Test
    @DisplayName("application*.yml 에 snake_case naming strategy 가 설정되어 있지 않다")
    void noSnakeCaseNamingStrategyConfigured() throws Exception {
        for (String yml : List.of("/application.yml", "/application-prod.yml", "/application-docker.yaml")) {
            try (InputStream in = getClass().getResourceAsStream(yml)) {
                if (in == null) {
                    continue; // 프로파일 파일이 없을 수 있음
                }
                String text = new String(in.readAllBytes());
                // property-naming-strategy 가 있고 그 값이 SNAKE 계열이면 위반
                boolean snake = text.matches("(?s).*property-naming-strategy\\s*:\\s*.*SNAKE.*")
                        || text.contains("SNAKE_CASE");
                assertThat(snake)
                        .as("%s 에 snake_case Jackson naming strategy 가 설정됨 — 와이어 camelCase 계약이 깨진다", yml)
                        .isFalse();
            }
        }
    }

    private void collectNonCamelKeys(JsonNode node, String path, List<String> out) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> e = it.next();
                String key = e.getKey();
                if (!CAMEL.matcher(key).matches()) {
                    out.add(path + "." + key);
                }
                collectNonCamelKeys(e.getValue(), path + "." + key, out);
            }
        } else if (node.isArray()) {
            int i = 0;
            for (JsonNode child : node) {
                collectNonCamelKeys(child, path + "[" + (i++) + "]", out);
            }
        }
    }
}
