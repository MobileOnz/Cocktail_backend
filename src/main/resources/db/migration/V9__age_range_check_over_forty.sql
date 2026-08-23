-- V9: AgeRange 에 OVER_FORTY 추가에 따른 CHECK 제약 확장 (QA P2-7)
-- AgeRange enum 은 @Enumerated 미지정 → ORDINAL(smallint) 로 저장된다.
-- '40세 이상'(프론트 '40_over')을 OVER_FORTY 로 바로잡으며 enum 맨 끝에 추가 → ordinal 6.
-- 기존 제약이 age_range 를 0~5 로 막고 있어 40대 온보딩 저장이 CHECK 위반(500)이 났다.
-- member/monitoring 두 테이블 모두 이 enum 을 ordinal 로 저장하므로 함께 상한을 6 으로 넓힌다.

ALTER TABLE monitoring DROP CONSTRAINT IF EXISTS monitoring_age_range_check;
ALTER TABLE monitoring ADD  CONSTRAINT monitoring_age_range_check CHECK (age_range >= 0 AND age_range <= 6);

ALTER TABLE member DROP CONSTRAINT IF EXISTS member_age_range_check;
ALTER TABLE member ADD  CONSTRAINT member_age_range_check CHECK (age_range >= 0 AND age_range <= 6);
