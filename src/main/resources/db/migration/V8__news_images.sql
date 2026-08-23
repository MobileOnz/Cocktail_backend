-- V8: 뉴스 썸네일 채우기 (QA P2-5)
-- V4 시드가 image_url 을 INSERT 목록에서 빼 전 항목 NULL → 메인/뉴스 카드가 이미지 없이 노출됐다.
-- V4 를 직접 수정하면 이미 적용된 기존 볼륨에서 Flyway checksum 이 깨지므로, append-only UPDATE 로 채운다.
-- 칵테일이 쓰는 것과 동일한 절대 S3 URL(onz-cocktail-images, 실제 200 확인됨)을 뉴스 주제에 맞춰 매핑.
-- image_url IS NULL 인 행만 갱신 → 재적용/QA 생성분(#13 등)에 안전. source_url 은 검증 가능한 값이 없어 NULL 유지.

UPDATE news SET image_url = 'https://onz-cocktail-images.s3.ap-northeast-2.amazonaws.com/cocktails/onz_cocktail_Daiquiri.png'        WHERE id = 1  AND image_url IS NULL;
UPDATE news SET image_url = 'https://onz-cocktail-images.s3.ap-northeast-2.amazonaws.com/cocktails/onz_cocktail_Dry_Martini.png'      WHERE id = 2  AND image_url IS NULL;
UPDATE news SET image_url = 'https://onz-cocktail-images.s3.ap-northeast-2.amazonaws.com/cocktails/onz_cocktail_Espresso_Martini.png' WHERE id = 3  AND image_url IS NULL;
UPDATE news SET image_url = 'https://onz-cocktail-images.s3.ap-northeast-2.amazonaws.com/cocktails/onz_cocktail_Negroni.png'          WHERE id = 4  AND image_url IS NULL;
UPDATE news SET image_url = 'https://onz-cocktail-images.s3.ap-northeast-2.amazonaws.com/cocktails/onz_cocktail_Old_Fashioned.png'    WHERE id = 5  AND image_url IS NULL;
UPDATE news SET image_url = 'https://onz-cocktail-images.s3.ap-northeast-2.amazonaws.com/cocktails/onz_cocktail_Whiskey_Sour.png'     WHERE id = 6  AND image_url IS NULL;
UPDATE news SET image_url = 'https://onz-cocktail-images.s3.ap-northeast-2.amazonaws.com/cocktails/onz_cocktail_Manhattan.png'        WHERE id = 7  AND image_url IS NULL;
UPDATE news SET image_url = 'https://onz-cocktail-images.s3.ap-northeast-2.amazonaws.com/cocktails/onz_cocktail_Moscow_Mule.png'      WHERE id = 8  AND image_url IS NULL;
UPDATE news SET image_url = 'https://onz-cocktail-images.s3.ap-northeast-2.amazonaws.com/cocktails/onz_cocktail_French_Martini.png'   WHERE id = 9  AND image_url IS NULL;
UPDATE news SET image_url = 'https://onz-cocktail-images.s3.ap-northeast-2.amazonaws.com/cocktails/onz_cocktail_Mojito.png'           WHERE id = 10 AND image_url IS NULL;
UPDATE news SET image_url = 'https://onz-cocktail-images.s3.ap-northeast-2.amazonaws.com/cocktails/onz_cocktail_Margarita.png'        WHERE id = 11 AND image_url IS NULL;
UPDATE news SET image_url = 'https://onz-cocktail-images.s3.ap-northeast-2.amazonaws.com/cocktails/onz_cocktail_Grand_Margarita.png'  WHERE id = 12 AND image_url IS NULL;

-- 잔여 안전망: 위 매핑에 안 걸린(향후 시드로 늘어난) NULL 뉴스도 기본 이미지로 채워 카드가 비지 않게 한다.
UPDATE news SET image_url = 'https://onz-cocktail-images.s3.ap-northeast-2.amazonaws.com/cocktails/onz_cocktail_Daiquiri.png'
WHERE image_url IS NULL;
