-- 테스트용 Mock 데이터 삽입 스크립트
-- 배치 즐겨찾기 API 테스트를 위한 데이터

-- 1. 테스트 회원 삽입
-- role: 0=ADMIN, 1=USER, 2=OTHER
INSERT INTO member (
    credential_id, name, nickname, email, social_login, role,
    age_term, service_term, marketing_term, ad_term,
    created_at, updated_at
) VALUES
(
    'test_user_001',
    '테스트유저',
    '테스터',
    'test@example.com',
    'KAKAO',
    1,
    true,
    true,
    false,
    false,
    NOW(),
    NOW()
)
ON CONFLICT (credential_id) DO NOTHING;

-- 2. 테스트 칵테일 삽입 (10개)
-- abv_band: "약함", "보통", "강함" (한글)
-- taste_level: "BEGINNER", "INTERMEDIATE", "ADVANCED" (enum name)
INSERT INTO cocktail (
    kor_name, eng_name, abv_band, max_alcohol, min_alcohol,
    taste_level, origin_text, image_url,
    recommend_count, hard_count,
    created_at, updated_at
) VALUES
(
    '모히또', 'Mojito', '약함', 15, 10,
    'BEGINNER', '쿠바 전통 칵테일로 민트와 라임의 상큼함이 특징입니다.',
    'https://example.com/images/mojito.jpg',
    0, 0, NOW(), NOW()
),
(
    '마가리타', 'Margarita', '보통', 25, 20,
    'INTERMEDIATE', '멕시코의 대표 칵테일로 데킬라와 라임의 조화가 인상적입니다.',
    'https://example.com/images/margarita.jpg',
    0, 0, NOW(), NOW()
),
(
    '롱아일랜드 아이스티', 'Long Island Iced Tea', '강함', 35, 30,
    'ADVANCED', '여러 증류주가 들어간 고도수 칵테일입니다.',
    'https://example.com/images/long_island.jpg',
    0, 0, NOW(), NOW()
),
(
    '피나 콜라다', 'Pina Colada', '약함', 18, 12,
    'BEGINNER', '파인애플과 코코넛의 열대 느낌이 물씬 나는 칵테일입니다.',
    'https://example.com/images/pina_colada.jpg',
    0, 0, NOW(), NOW()
),
(
    '맨해튼', 'Manhattan', '보통', 30, 25,
    'INTERMEDIATE', '위스키 베이스의 클래식 칵테일입니다.',
    'https://example.com/images/manhattan.jpg',
    0, 0, NOW(), NOW()
),
(
    '블루 하와이', 'Blue Hawaii', '약함', 16, 12,
    'BEGINNER', '블루 큐라소의 아름다운 색상이 특징인 열대 칵테일입니다.',
    'https://example.com/images/blue_hawaii.jpg',
    0, 0, NOW(), NOW()
),
(
    '올드 패션드', 'Old Fashioned', '강함', 38, 35,
    'ADVANCED', '가장 오래된 클래식 칵테일 중 하나입니다.',
    'https://example.com/images/old_fashioned.jpg',
    0, 0, NOW(), NOW()
),
(
    '코스모폴리탄', 'Cosmopolitan', '보통', 22, 18,
    'INTERMEDIATE', '보드카 베이스의 세련된 핑크색 칵테일입니다.',
    'https://example.com/images/cosmopolitan.jpg',
    0, 0, NOW(), NOW()
),
(
    '진 토닉', 'Gin and Tonic', '약함', 15, 10,
    'BEGINNER', '진과 토닉워터의 간단하지만 완벽한 조화입니다.',
    'https://example.com/images/gin_tonic.jpg',
    0, 0, NOW(), NOW()
),
(
    '네그로니', 'Negroni', '보통', 28, 24,
    'INTERMEDIATE', '이탈리아의 대표적인 비터 칵테일입니다.',
    'https://example.com/images/negroni.jpg',
    0, 0, NOW(), NOW()
)
ON CONFLICT DO NOTHING;

-- 삽입된 데이터 확인
SELECT 'Member 테이블 확인' as status, COUNT(*) as count FROM member WHERE credential_id = 'test_user_001';
SELECT 'Cocktail 테이블 확인' as status, COUNT(*) as count FROM cocktail;