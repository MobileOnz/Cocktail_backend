-- 매거진: 전쟁 구호가 된 칵테일, 리멤버 더 메인
--
-- 이 글은 어드민으로 프로덕션 DB 에 직접 들어갔고 레포에는 없었다. 그래서 로컬과
-- 새 환경에서는 존재하지 않았고("로컬에서 글이 사라졌다"의 실체가 이것이다),
-- 콘텐츠가 코드 리뷰도 롤백도 되지 않는 상태로 운영 DB 에만 떠 있었다.
-- 앞으로 어드민으로 넣은 글도 여기로 회수한다 — 콘텐츠도 버전 관리 대상이다.
--
-- 프로덕션에는 이 글이 id 305 · slug remember-the-maine 으로 이미 들어가 있다.
-- 그래서 재적용 시 기본키와 slug 유니크 인덱스가 **둘 다** 충돌한다.
-- ON CONFLICT (slug) 처럼 대상을 지정하면 그 인덱스 위반만 잡아내므로,
-- 기본키가 먼저 걸리면 잡지 못하고 마이그레이션이 통째로 실패한다.
-- 대상을 비운 ON CONFLICT DO NOTHING 은 어느 유니크 제약이 걸리든 넘어간다.

INSERT INTO magazine_article (
    id, slug, title, title_lines, dek, category, subcategory,
    cover_image, hero_image, thumbnail, image_caption,
    author_name, content, refs, reading_time_min,
    status, published_at
) VALUES (
    305,
    'remember-the-maine',
    '전쟁 구호가 된 칵테일, 리멤버 더 메인',
    '["전쟁 구호가 된 칵테일,", "리멤버 더 메인"]'::jsonb,
    '바에서 이 이름을 시키는 사람은 있어도, 이게 진짜 전쟁을 부른 구호였다는 걸 아는 사람은 드물어요. 게다가 이 술은 그 이름이 붙기 전부터, 다른 이름으로 살아 있었습니다.',
    'STORY',
    '클래식',
    'http://onz-cocktail.kr/uploads/cocktails/onz_cocktail_Remember_the_Maine.webp',
    'http://onz-cocktail.kr/uploads/cocktails/onz_cocktail_Remember_the_Maine.webp',
    'http://onz-cocktail.kr/uploads/cocktails/onz_cocktail_Remember_the_Maine.webp',
    '리멤버 더 메인, 라이 위스키에 체리와 압생트를 더한 맨해튼의 사촌',
    'onz 에디터',
    '[{"text": "바에서 ''리멤버 더 메인(Remember the Maine)''을 주문하는 사람은 가끔 있어요. 그런데 이 이름이 100여 년 전 진짜 전쟁을 부른 구호였다는 걸 아는 사람은 드물죠. 더 이상한 건, 이 칵테일이 그 이름으로 불리기 전부터 전혀 다른 이름으로 이미 살아 있었다는 거예요. 한 잔 안에 폭발한 군함, 신문사의 거짓말, 그리고 대통령 한 명이 접혀 있습니다. 순서대로 풀어볼게요.", "type": "paragraph"}, {"text": "자다가 스러진 사람들, 그리고 신문이 시작한 전쟁", "type": "heading", "level": 2}, {"text": "확인되지 않은 기사 한 줄이 진짜 전쟁을 만든 이야기예요. 1898년 2월 15일 밤 9시 40분, 쿠바 아바나 항. 정박해 있던 미국 군함 메인호가 갑자기 폭발합니다. 6인치와 10인치 포탄용 화약 5톤 넘게가 한꺼번에 터지면서 뱃머리 3분의 1이 통째로 날아갔어요. 하필 대부분의 승조원이 앞쪽 병사 숙소에서 자고 있던 시간이었습니다. 배에 있던 354명 가운데 4분의 3에 가까운, 260명이 넘는 사람이 그 자리에서 돌아오지 못했어요.", "type": "paragraph", "marks": [{"text": "확인되지 않은 기사 한 줄이 진짜 전쟁을 만든 이야기예요.", "style": "bold"}]}, {"text": "원인은 그날 밤엔 아무도 몰랐습니다. 그런데 미국 신문들이 먼저 움직였어요. 허스트의 뉴욕 저널과 퓰리처의 뉴욕 월드가 판매 부수를 놓고 치고받던 시절이었고, 두 신문은 증거도 없이 스페인을 범인으로 지목했습니다. 특히 허스트의 저널이 더 공격적이었죠. ''메인호를 기억하라, 스페인에 지옥을(Remember the Maine, to hell with Spain).'' 이 한 줄이 신문을 팔았고, 여론을 끓게 했고, 몇 달 뒤 미국을 스페인과의 전쟁으로 밀어넣었어요.", "type": "paragraph"}, {"text": "78년 뒤에 온 반전, 스페인은 범인이 아니었다", "type": "heading", "level": 2}, {"text": "그런데 78년이 지나 반전이 옵니다. 스페인은 애초에 범인이 아니었을지도 몰라요. 신문이 팔아먹은 그 전제가 통째로 틀렸을 수 있다는 거죠. 1976년, 핵잠수함의 아버지로 불리는 하이먼 리코버 제독이 이 사건을 다시 팠어요. 리코버는 해군이 뭔가를 덮은 게 아닐까 하는 의심을 오래 품고 있었거든요. 그는 역사가와 엔지니어로 팀을 꾸려 폭발을 재분석했고, 결론은 이랬어요. 스페인의 기뢰가 아니라, 석탄고에 난 불이 옆 탄약고를 건드린 사고. 즉 배는 밖이 아니라 안에서 터졌다는 겁니다. 리코버가 1976년 펴낸 책 제목이 그대로 『전함 메인호는 어떻게 파괴되었나』였고요.", "type": "paragraph", "marks": [{"text": "스페인은 애초에 범인이 아니었을지도 몰라요.", "style": "bold"}]}, {"text": "웃긴 건, 78년간 세 번이나 조사했는데 앞의 두 번(1898년, 1911년)은 전부 ''외부 폭발'', 그러니까 스페인 쪽으로 기울었다는 거예요. 마지막 리코버의 조사가 그걸 다 뒤집었죠. 그사이 전쟁은 이미 한참 전에 끝나 있었고요.", "type": "paragraph"}, {"text": "폭탄이 떨어지던 아바나의 밤, 술이 된 이름", "type": "heading", "level": 2}, {"text": "''리멤버 더 메인''이라는 이름으로 이 칵테일이 기록된 건 1898년이 아니라 1939년, 미국 작가 찰스 베이커의 책 『신사의 동반자(The Gentleman''s Companion)』에서예요. 그런데 그가 이 술을 실제로 만난 밤은 그보다 앞선 1933년 쿠바, 하필 또 혁명의 한복판이었죠. 베이커는 폭탄이 떨어지는 아바나에서 이 술을 마시며 이렇게 적었어요.", "type": "paragraph"}, {"cite": "찰스 베이커, 『신사의 동반자』(1939)", "text": "1933년 어수선하던 아바나의 어느 밤에 대한 흐릿한 기억. 한 모금 한 모금이 프라도 거리에서 터지는 폭탄 소리, 혹은 나시오날 호텔로 날아드는 3인치 포탄 소리에 끊겼다.", "type": "quote"}, {"text": "세계를 떠돌며 각 도시의 술과 음식을 기록한, 요즘으로 치면 원조 미식 여행 작가다운 문장이죠. 술 한 잔에 이런 걸 남기다니 좀 멋있지 않나요. 게다가 베이커는 이 칵테일을 꼭 시계방향으로 저으라고 고집했어요. 그래야 ''바다에 나가도 견딘다(sea-worthy)''면서요. 근거는 없지만 이름에 어울리는 미신이죠.", "type": "paragraph"}, {"text": "사실 이 술은, 전쟁을 선포한 대통령의 이름이었다", "type": "heading", "level": 2}, {"text": "여기서 진짜 반전이 하나 더 있어요. 리멤버 더 메인은 그 이름이 붙기 전부터 다른 이름으로 살아 있었습니다. 1896년 뉴욕의 옛 월도프-아스토리아 바에서 태어난 ''매킨리스 딜라이트(McKinley''s Delight)''가 그것이에요. 라이 위스키에 스위트 베르무트, 체리 브랜디 두 방울, 압생트 한 방울. 리멤버 더 메인과 사실상 같은 레시피입니다. 앨버트 크로켓의 『Old Waldorf Bar Days』(1931)에 이 이름으로 실려 있어요.", "type": "paragraph", "marks": [{"text": "리멤버 더 메인은 그 이름이 붙기 전부터 다른 이름으로 살아 있었습니다.", "style": "bold"}]}, {"text": "그런데 매킨리가 누구냐면, 1898년 메인호가 폭발했을 때 미국 대통령이자 스페인에 전쟁을 선포한 바로 그 사람입니다. 그러니까 이 술엔 같은 사람이 두 번 새겨진 셈이에요. 한 번은 그의 이름으로(매킨리스 딜라이트), 한 번은 그가 선포한 전쟁의 구호로(리멤버 더 메인). 정파색 짙은 원래 이름은 점점 잊혔고, 애국적 추모의 새 이름만 클래식으로 살아남았고요.", "type": "paragraph"}, {"text": "잔에 코를 대면 압생트 향이 먼저 훅 들어와요. 정작 압생트는 잔 안쪽에 살짝 바르고 따라버린 건데, 그 얇은 한 겹이 첫인상을 다 만들죠. 한 모금 넘기면 ''맨해튼인가'' 싶다가, 뒤에서 체리 브랜디가 단맛으로 붙잡아요. 실제로 라이 위스키에 스위트 베르무트라는 뼈대가 맨해튼과 똑같고, 거기에 체리와 압생트를 얹어 한 단계 더 스파이시하게 간 거예요. 도수는 30도쯤, 얼음에 젓기만 하고 흔들진 않아요. 향과 질감이 흐트러지니까요.", "type": "paragraph"}, {"name": "리멤버 더 메인", "type": "cocktail_spec", "image": "http://onz-cocktail.kr/uploads/cocktails/onz_cocktail_Remember_the_Maine.webp", "specs": [{"k": "베이스", "v": "라이 위스키"}, {"k": "도수", "v": "약 30%"}, {"k": "스타일", "v": "스터드·스피릿"}, {"k": "키 리큐어", "v": "체리·압생트"}], "name_en": "Remember the Maine", "cocktail_id": null}, {"text": "이름 하나에 접힌 것들", "type": "heading", "level": 2}, {"text": "조용한 바 한구석에서 이 잔을 시킬 때, 이름 하나에 얼마나 많은 게 접혀 있는지 생각하면 묘해요. 아바나 항에서 자다가 스러진 260명, 그들을 팔아 전쟁을 부른 신문 한 줄, 78년 뒤 그 거짓말을 걷어낸 노제독, 폭탄이 떨어지던 밤 술잔을 기울이던 여행 작가, 그리고 그 모든 일의 한복판에 있던 대통령까지.", "type": "paragraph"}, {"text": "''알고 마시면 다르다''는 말이 이만큼 잘 맞는 잔도 드물어요. 다음에 바에서 이 이름을 보면, 베이커처럼 시계방향으로 한 번 저어보세요. 바다에 나가도 견딜 수 있도록.", "type": "paragraph"}]'::jsonb,
    '{"sources": [{"url": "https://en.wikipedia.org/wiki/USS_Maine_(1890)", "title": "USS Maine (1890) — Wikipedia"}, {"url": "https://en.wikipedia.org/wiki/Remember_the_Maine_(cocktail)", "title": "Remember the Maine (cocktail) — Wikipedia"}, {"url": "https://www.diffordsguide.com/cocktails/recipe/1668/remember-the-maine", "title": "Remember the Maine — Difford''s Guide"}, {"url": "https://www.diffordsguide.com/cocktails/recipe/7653/mckinleys-delight", "title": "McKinley''s Delight — Difford''s Guide"}, {"url": "https://historymatters.gmu.edu/d/5470/", "title": "Rickover 1976 investigation clears Spain — History Matters (GMU)"}, {"url": "https://punchdrink.com/recipes/remember-the-maine/", "title": "Remember the Maine — PUNCH"}], "cocktail_ids": [], "related_article_slugs": ["tipperary", "naked-and-famous", "tuxedo"]}'::jsonb,
    7,
    'PUBLISHED',
    '2026-07-13T10:00:00'
)
ON CONFLICT DO NOTHING;

-- 태그. tag 는 칵테일과 공유하는 테이블이고 type 에 CHECK 가 걸려 있다
-- (FLAVOR / MOOD / BASE / GLASS). 없는 이름만 새로 만들고 이름으로 이어붙인다.
INSERT INTO tag (name, type) VALUES ('위스키', 'BASE')
ON CONFLICT (name) DO NOTHING;
INSERT INTO tag (name, type) VALUES ('클래식', 'MOOD')
ON CONFLICT (name) DO NOTHING;

INSERT INTO magazine_article_tag (article_id, tag_id)
SELECT a.id, t.id
FROM magazine_article a
JOIN tag t ON t.name IN ('위스키', '클래식')
WHERE a.slug = 'remember-the-maine'
ON CONFLICT (article_id, tag_id) DO NOTHING;

-- 식별자 시퀀스가 305 아래에 있으면 다음 INSERT 가 충돌한다.
SELECT setval(
    pg_get_serial_sequence('magazine_article', 'id'),
    GREATEST((SELECT MAX(id) FROM magazine_article), 305)
);
