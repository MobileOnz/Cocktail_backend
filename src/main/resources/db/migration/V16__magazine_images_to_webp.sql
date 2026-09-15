-- V16: 매거진 이미지를 S3 PNG → EC2 WebP 로.
--
-- V14 가 cocktail / guide / news 를 옮길 때 magazine_article 이 빠져 있었다.
-- 그래서 매거진 24건 중 12건(id 1~12)이 아직 죽은 줄 알았던 S3 버킷을 보고 있다.
-- 버킷이 살아 있어 이미지가 깨지지는 않지만, 앱이 매거진 목록을 열 때마다
-- 716KB 짜리 PNG 를 12장 받는다(약 8.6MB). 같은 그림의 WebP 는 평균 65KB 다.
--
-- QA: "매거진 스토리 홈 사진이 느리게 뜬다", "홈화면 딜레이 걸림".
--
-- 파일은 이미 /var/onz/uploads/cocktails/ 에 전부 있다(12장 확인). 이 마이그레이션은
-- 가리키는 주소만 바꾼다. 변환 규칙은 V14 와 동일 — 점(.)은 언더스코어로.
UPDATE magazine_article
SET cover_image = 'http://onz-cocktail.kr/uploads/cocktails/'
    || replace(regexp_replace(split_part(cover_image, '/cocktails/', 2), '\.png$', ''), '.', '_')
    || '.webp'
WHERE cover_image LIKE '%onz-cocktail-images.s3%/cocktails/%';

UPDATE magazine_article
SET hero_image = 'http://onz-cocktail.kr/uploads/cocktails/'
    || replace(regexp_replace(split_part(hero_image, '/cocktails/', 2), '\.png$', ''), '.', '_')
    || '.webp'
WHERE hero_image LIKE '%onz-cocktail-images.s3%/cocktails/%';

UPDATE magazine_article
SET thumbnail = 'http://onz-cocktail.kr/uploads/cocktails/'
    || replace(regexp_replace(split_part(thumbnail, '/cocktails/', 2), '\.png$', ''), '.', '_')
    || '.webp'
WHERE thumbnail LIKE '%onz-cocktail-images.s3%/cocktails/%';
