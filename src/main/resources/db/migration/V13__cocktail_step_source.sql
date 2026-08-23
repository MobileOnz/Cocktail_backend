-- V13: 제조 단계의 출처 구분.
--
-- 105종 중 12종(11%)만 손으로 쓴 단계를 갖고 있어 나머지 93종은 "만드는 법"이 비어 있다.
-- 재료·잔·스타일에서 규칙으로 초안을 생성해 채우되, 사람이 쓴 것과 반드시 구분해야 한다.
--   MANUAL — 사람이 작성. 생성기가 절대 건드리지 않는다.
--   AUTO   — 규칙으로 생성한 초안. 재생성 시 덮어쓴다.
ALTER TABLE cocktail_step
    ADD COLUMN IF NOT EXISTS source VARCHAR(16) NOT NULL DEFAULT 'MANUAL';

-- 기존 46행은 전부 사람이 쓴 것이다.
UPDATE cocktail_step SET source = 'MANUAL' WHERE source IS NULL;

CREATE INDEX IF NOT EXISTS idx_cocktail_step_source ON cocktail_step(source);
