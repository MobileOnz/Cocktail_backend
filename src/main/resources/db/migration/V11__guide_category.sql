-- 가이드 카테고리. 상단 종류별 탭(FE)이 이 값으로 동적 구성된다.
-- 값은 어드민에서 자유 입력 — 새 카테고리를 추가해도 앱 업데이트가 필요 없다.
ALTER TABLE guide ADD COLUMN category VARCHAR(50);

COMMENT ON COLUMN guide.category IS '가이드 카테고리(탭 라벨). NULL 이면 FE 가 기타로 묶는다';

-- 기존 12편 백필: part 백의 자리가 사실상의 묶음이었다 (1XX/2XX/3XX).
UPDATE guide SET category = CASE
    WHEN part < 200 THEN '입문'
    WHEN part < 300 THEN '만들기'
    ELSE '실전'
END;
