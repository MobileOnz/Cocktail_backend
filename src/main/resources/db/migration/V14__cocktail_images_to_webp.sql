-- V14: 칵테일·잔 이미지를 S3 PNG → EC2 WebP 로.
--
-- 2026-05-01 에 이미지가 S3 에서 EC2(/uploads, nginx 직접 서빙)로 이관됐고 프로덕션은 이미 WebP 다.
-- 그런데 로컬 시드(remote_db_dump.sql, 2026-04-12)가 이관 이전 덤프라 로컬만 S3 PNG 로 남아 있었다.
-- QA 빌드가 로컬 백엔드를 보므로 폰에서도 옛 PNG 를 받는다 — 그래서 데이터로 맞춘다.
--
-- 용량 실측(아메리카노):
--   칵테일  1,485,744 B (PNG) → 37,780 B (WebP)   39배
--   잔        324,697 B (PNG) →  7,752 B (WebP)   42배
--   상세 진입 기준 약 1.81MB → 45KB
--
-- 프로덕션에서는 이미 WebP 라 WHERE 절에 걸리는 행이 없다(무해한 no-op).
-- 파일명은 프로덕션 규칙을 그대로 따른다: 점(.)은 언더스코어로 (예: Ve.N.To → Ve_N_To).
-- https 는 이 호스트에서 열려 있지 않아 프로덕션과 동일하게 http 를 쓴다.

UPDATE cocktail
SET image_url = 'http://onz-cocktail.kr/uploads/cocktails/'
    || replace(regexp_replace(split_part(image_url, '/cocktails/', 2), '\.png$', ''), '.', '_')
    || '.webp'
WHERE image_url LIKE '%onz-cocktail-images.s3%/cocktails/%';

UPDATE cocktail
SET glass_image_url = 'http://onz-cocktail.kr/uploads/glasses/'
    || replace(regexp_replace(split_part(glass_image_url, '/glasses/', 2), '\.png$', ''), '.', '_')
    || '.webp'
WHERE glass_image_url LIKE '%onz-cocktail-images.s3%/glasses/%';

-- 뉴스도 같은 S3 PNG 를 쓰고 있었다(V8 이 칵테일 이미지를 그대로 재사용했다).
UPDATE news
SET image_url = 'http://onz-cocktail.kr/uploads/cocktails/'
    || replace(regexp_replace(split_part(image_url, '/cocktails/', 2), '\.png$', ''), '.', '_')
    || '.webp'
WHERE image_url LIKE '%onz-cocktail-images.s3%/cocktails/%';
