-- V7: 회원 선호도(member_preference) + 가이드↔칵테일 매핑(guide_cocktail_map) (T-09)
-- 컬럼 snake_case, 응답 JSON camelCase. ddl-auto:validate 정합.
-- (V7 하나만 소유하므로 두 테이블을 함께 담는다.)

-- ── 회원 선호도: 온보딩/추천 결과로 갱신되는 개인화 캐시 ─────────────────
CREATE TABLE member_preference (
    member_id   BIGINT PRIMARY KEY REFERENCES member(id) ON DELETE CASCADE,
    base_spirit VARCHAR(30),   -- 선호 베이스(진/럼/보드카/위스키/테킬라...)
    sweetness   VARCHAR(12),   -- SWEET / BALANCED / DRY
    abv_range   VARCHAR(12),   -- LOW / MID / HIGH  (약함/보통/강함)
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ── 가이드 ↔ 칵테일 매핑: '이 칵테일의 이야기' ──────────────────────────
CREATE TABLE guide_cocktail_map (
    guide_part  INTEGER NOT NULL REFERENCES guide(part) ON DELETE CASCADE,
    cocktail_id BIGINT  NOT NULL REFERENCES cocktail(id) ON DELETE CASCADE,
    PRIMARY KEY (guide_part, cocktail_id)
);

-- ═══════════════════════════════════════════════════════════════════════
-- 시드: guide_cocktail_map (kor_name 매핑 — id 비의존, 그럴듯한 연결)
-- ═══════════════════════════════════════════════════════════════════════
INSERT INTO guide_cocktail_map (guide_part, cocktail_id)
SELECT v.part, c.id
FROM cocktail c
JOIN (VALUES
    (101, '아메리카노'), (101, '진 피즈'),
    (102, '마가리타'),   (102, '다이키리'),
    (103, '위스키 사워'),(103, '진 피즈'),
    (201, '모스코 뮬'),  (201, '아페롤 스프리츠'),
    (202, '모히또'),     (202, '다이키리'),
    (203, '모히또'),     (203, '마가리타'), (203, '코스모폴리탄'),
    (204, '블랙 러시안'),(204, '위스키 사워'),
    (205, '다이키리'),   (205, '진 피즈'), (205, '블랙 러시안'), (205, '마가리타'),
    (301, '모스코 뮬'),  (301, '블랙 러시안'), (301, '카이피리냐'),
    (302, '아페롤 스프리츠'), (302, '코스모폴리탄'),
    (303, '아메리카노'), (303, '진 피즈'), (303, '모히또'),
    (304, '다이키리'),   (304, '프렌치 75')
) AS v(part, kor_name) ON c.kor_name = v.kor_name
-- guide 구성은 환경마다 다르다. 실제로 없는 part 를 넣으면 FK 위반으로
-- 마이그레이션 전체가 멈춘다(프로덕션에 205 가 없어 실제로 겪었다).
-- 존재하는 part 만 넣어 어느 환경에서도 통과하게 한다.
WHERE EXISTS (SELECT 1 FROM guide g WHERE g.part = v.part);
