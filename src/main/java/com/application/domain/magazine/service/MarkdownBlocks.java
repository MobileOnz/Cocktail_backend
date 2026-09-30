package com.application.domain.magazine.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 어드민 마크다운 본문 ↔ 매거진 블록(content JSONB) 변환.
 *
 * 편집자는 마크다운으로 쓰고, 앱(MagazineBlockRenderer)은 블록만 그린다.
 * 지원: ## 소제목 · 문단(**굵게**) · 이미지(+바로 아래 *기울임* 한 줄은 캡션) · > 인용(마지막 (출처) 줄은 cite) · 표 → 스펙 카드.
 * 본문 맨 앞 이미지는 블록에서 빼고 대표 이미지로 쓴다 — 상세 화면이 대표 이미지를 따로 그리므로 두 번 보이지 않게.
 */
public final class MarkdownBlocks {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Pattern IMAGE = Pattern.compile("^!\\[([^\\]]*)]\\(([^)\\s]+)\\)\\s*$");
    private static final Pattern CAPTION = Pattern.compile("^\\*([^*].*)\\*$");
    private static final Pattern HEADING = Pattern.compile("^(#{2,4})\\s+(.*)$");
    private static final Pattern LINK = Pattern.compile("\\[([^\\]]+)]\\([^)]+\\)");
    private static final Pattern BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern ITALIC = Pattern.compile("(?<!\\*)\\*([^*]+)\\*(?!\\*)");
    private static final Pattern CITE = Pattern.compile("^[(（](.*)[)）]$");
    private static final Pattern TABLE_RULE = Pattern.compile("^:?-{2,}:?$");

    private MarkdownBlocks() {}

    /** 변환 결과. heroImage/heroCaption 은 본문 첫 이미지(없으면 null). */
    public record Result(String contentJson, String heroImage, String heroCaption, int charCount) {}

    public static Result toBlocks(String markdown) {
        String[] lines = (markdown == null ? "" : markdown).replace("\r\n", "\n").split("\n", -1);
        ArrayNode blocks = MAPPER.createArrayNode();
        String heroImage = null;
        String heroCaption = null;
        int chars = 0;

        int i = 0;
        while (i < lines.length) {
            String line = lines[i].strip();
            if (line.isEmpty() || line.matches("^-{3,}$")) {
                i++;
                continue;
            }

            Matcher img = IMAGE.matcher(line);
            if (img.matches()) {
                String caption = null;
                if (i + 1 < lines.length) {
                    Matcher cap = CAPTION.matcher(lines[i + 1].strip());
                    if (cap.matches()) {
                        caption = cap.group(1).strip();
                        i++;
                    }
                }
                if (heroImage == null && blocks.isEmpty()) {
                    heroImage = img.group(2);
                    heroCaption = caption;
                } else {
                    ObjectNode b = blocks.addObject().put("type", "image").put("src", img.group(2));
                    if (caption != null) b.put("caption", caption);
                    if (heroImage == null) {
                        heroImage = img.group(2);
                        heroCaption = caption;
                    }
                }
                i++;
                continue;
            }

            Matcher h = HEADING.matcher(line);
            if (h.matches()) {
                String text = plain(h.group(2));
                blocks.addObject().put("type", "heading").put("level", h.group(1).length()).put("text", text);
                chars += text.length();
                i++;
                continue;
            }

            if (line.startsWith(">")) {
                List<String> quote = new ArrayList<>();
                while (i < lines.length && lines[i].strip().startsWith(">")) {
                    String q = lines[i].strip().substring(1).strip();
                    if (!q.isEmpty()) quote.add(q);
                    i++;
                }
                String cite = null;
                if (quote.size() > 1) {
                    Matcher c = CITE.matcher(quote.get(quote.size() - 1));
                    if (c.matches()) {
                        cite = c.group(1).strip();
                        quote.remove(quote.size() - 1);
                    }
                }
                String text = plain(String.join(" ", quote));
                ObjectNode b = blocks.addObject().put("type", "quote").put("text", text);
                if (cite != null) b.put("cite", cite);
                chars += text.length();
                continue;
            }

            if (line.startsWith("|")) {
                List<String[]> rows = new ArrayList<>();
                boolean hasHeader = false;
                while (i < lines.length && lines[i].strip().startsWith("|")) {
                    String[] cells = lines[i].strip().replaceAll("^\\||\\|$", "").split("\\|", -1);
                    i++;
                    if (TABLE_RULE.matcher(cells[0].strip()).matches()) {
                        // 구분선(|---|) 위의 행은 머리행이라 스펙이 아니다.
                        hasHeader = rows.size() == 1;
                        continue;
                    }
                    if (cells.length >= 2) rows.add(cells);
                }
                ArrayNode specs = MAPPER.createArrayNode();
                for (String[] cells : rows.subList(hasHeader ? 1 : 0, rows.size())) {
                    specs.addObject().put("k", plain(cells[0])).put("v", plain(cells[1]));
                }
                if (!specs.isEmpty()) {
                    blocks.addObject().put("type", "cocktail_spec").set("specs", specs);
                }
                continue;
            }

            // 문단: 빈 줄이 나올 때까지 이어지는 줄은 한 문단이다.
            StringBuilder para = new StringBuilder(line);
            i++;
            while (i < lines.length && isContinuation(lines[i].strip())) {
                para.append(' ').append(lines[i].strip());
                i++;
            }
            String raw = LINK.matcher(para.toString()).replaceAll("$1");
            List<String> bolds = new ArrayList<>();
            Matcher bm = BOLD.matcher(raw);
            while (bm.find()) bolds.add(bm.group(1));
            String text = plain(raw);
            ObjectNode b = blocks.addObject().put("type", "paragraph").put("text", text);
            if (!bolds.isEmpty()) {
                ArrayNode marks = b.putArray("marks");
                for (String bold : bolds) marks.addObject().put("text", plain(bold)).put("style", "bold");
            }
            chars += text.length();
        }

        return new Result(blocks.toString(), heroImage, heroCaption, chars);
    }

    private static boolean isContinuation(String line) {
        return !line.isEmpty() && !line.startsWith(">") && !line.startsWith("|") && !line.startsWith("#")
                && !line.matches("^-{3,}$") && !IMAGE.matcher(line).matches();
    }

    private static String plain(String s) {
        String t = LINK.matcher(s).replaceAll("$1");
        t = BOLD.matcher(t).replaceAll("$1");
        t = ITALIC.matcher(t).replaceAll("$1");
        return t.strip();
    }

    /** 어드민 수정 화면용 역변환. 블록 → 마크다운(대표 이미지는 맨 앞에 되살린다). */
    public static String toMarkdown(String contentJson, String heroImage, String heroCaption) {
        StringBuilder md = new StringBuilder();
        if (heroImage != null && !heroImage.isBlank()) {
            md.append("![](").append(heroImage).append(")\n");
            if (heroCaption != null && !heroCaption.isBlank()) md.append('*').append(heroCaption).append("*\n");
            md.append('\n');
        }
        JsonNode blocks;
        try {
            blocks = MAPPER.readTree(contentJson == null ? "[]" : contentJson);
        } catch (Exception e) {
            return md.toString();
        }
        if (!blocks.isArray()) return md.toString();

        for (JsonNode b : blocks) {
            switch (b.path("type").asText()) {
                case "heading" -> md.append("#".repeat(Math.max(2, b.path("level").asInt(2))))
                        .append(' ').append(b.path("text").asText()).append("\n\n");
                case "paragraph" -> {
                    String text = b.path("text").asText();
                    for (JsonNode m : b.path("marks")) {
                        String bold = m.path("text").asText();
                        if ("bold".equals(m.path("style").asText()) && !bold.isEmpty()) {
                            text = text.replaceFirst(Pattern.quote(bold), Matcher.quoteReplacement("**" + bold + "**"));
                        }
                    }
                    md.append(text).append("\n\n");
                }
                case "image" -> {
                    md.append("![](").append(b.path("src").asText()).append(")\n");
                    if (b.hasNonNull("caption")) md.append('*').append(b.path("caption").asText()).append("*\n");
                    md.append('\n');
                }
                case "quote" -> {
                    md.append("> ").append(b.path("text").asText()).append('\n');
                    if (b.hasNonNull("cite")) md.append("> (").append(b.path("cite").asText()).append(")\n");
                    md.append('\n');
                }
                case "cocktail_spec" -> {
                    md.append("| 항목 | 내용 |\n|---|---|\n");
                    for (JsonNode s : b.path("specs")) {
                        md.append("| ").append(s.path("k").asText()).append(" | ").append(s.path("v").asText()).append(" |\n");
                    }
                    md.append('\n');
                }
                default -> { /* 마크다운으로 표현 못 하는 블록은 수정 화면에서 빠진다 */ }
            }
        }
        return md.toString().stripTrailing() + "\n";
    }
}
