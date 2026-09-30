package com.application.domain.magazine.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 어드민 마크다운 → 매거진 블록 변환.
 * 입력 샘플은 2026-09-29 에 실제로 발행된 글(리멤버 더 메인)의 형식을 줄여 옮긴 것이다.
 */
class MarkdownBlocksTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String SAMPLE = """
            ![폭발 뒤 아바나항](https://example.com/hero.jpg)
            *폭발 뒤 아바나항에 남은 메인호 잔해 · 퍼블릭 도메인*

            바 메뉴판에서 '리멤버 더 메인'을 처음 보면 이름 한번 비장하다 싶습니다.

            ## 자다가 스러진 사람들

            **확인되지 않은 기사 한 줄이 진짜 전쟁을 일으켰습니다.** 1898년 2월 15일 밤,
            아바나 항에서 [메인호](https://en.wikipedia.org/wiki/USS_Maine)가 폭발합니다.

            ![만평](https://example.com/press-war.jpg)
            *판매 부수 다툼이 전쟁을 밀었다*

            > "한 모금 한 모금이 폭탄 소리에 끊겼다."
            > (찰스 베이커, 『신사의 동반자』 1939)

            | 항목 | 내용 |
            |---|---|
            | 베이스 | 라이 위스키 |
            | 도수 | 약 30% |

            ---

            *이어서 읽어보세요: 티퍼러리*
            """;

    @Test
    @DisplayName("맨 앞 이미지는 대표 이미지로 빠지고, 나머지는 앱이 그리는 블록 타입으로 바뀐다")
    void convertsToRenderableBlocks() throws Exception {
        MarkdownBlocks.Result r = MarkdownBlocks.toBlocks(SAMPLE);
        JsonNode blocks = MAPPER.readTree(r.contentJson());

        assertThat(r.heroImage()).isEqualTo("https://example.com/hero.jpg");
        assertThat(r.heroCaption()).isEqualTo("폭발 뒤 아바나항에 남은 메인호 잔해 · 퍼블릭 도메인");

        assertThat(blocks).extracting(b -> b.path("type").asText()).containsExactly(
                "paragraph", "heading", "paragraph", "image", "quote", "cocktail_spec", "paragraph");

        JsonNode para = blocks.get(2);
        assertThat(para.path("text").asText())
                .doesNotContain("**", "](")
                .contains("1898년 2월 15일 밤, 아바나 항에서 메인호가 폭발합니다.");
        assertThat(para.path("marks").get(0).path("text").asText())
                .isEqualTo("확인되지 않은 기사 한 줄이 진짜 전쟁을 일으켰습니다.");

        assertThat(blocks.get(3).path("caption").asText()).isEqualTo("판매 부수 다툼이 전쟁을 밀었다");
        assertThat(blocks.get(4).path("cite").asText()).isEqualTo("찰스 베이커, 『신사의 동반자』 1939");

        JsonNode specs = blocks.get(5).path("specs");
        assertThat(specs).hasSize(2);
        assertThat(specs.get(0).path("k").asText()).isEqualTo("베이스");
        assertThat(specs.get(0).path("v").asText()).isEqualTo("라이 위스키");

        assertThat(blocks.get(6).path("text").asText()).isEqualTo("이어서 읽어보세요: 티퍼러리");
    }

    @Test
    @DisplayName("수정 화면용 역변환을 다시 변환하면 같은 블록이 나온다")
    void roundTrips() {
        MarkdownBlocks.Result first = MarkdownBlocks.toBlocks(SAMPLE);
        String md = MarkdownBlocks.toMarkdown(first.contentJson(), first.heroImage(), first.heroCaption());
        MarkdownBlocks.Result second = MarkdownBlocks.toBlocks(md);

        assertThat(second.contentJson()).isEqualTo(first.contentJson());
        assertThat(second.heroImage()).isEqualTo(first.heroImage());
        assertThat(second.heroCaption()).isEqualTo(first.heroCaption());
    }

    @Test
    @DisplayName("빈 본문도 유효한 JSON 배열이 된다")
    void emptyBody() {
        assertThat(MarkdownBlocks.toBlocks(null).contentJson()).isEqualTo("[]");
        assertThat(MarkdownBlocks.toBlocks("   \n\n").contentJson()).isEqualTo("[]");
    }
}
