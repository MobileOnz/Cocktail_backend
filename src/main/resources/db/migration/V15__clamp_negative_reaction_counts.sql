-- V15: 음수로 내려간 리액션 카운트를 0 으로 정규화.
--
-- decrementRecommend / decrementHard 에 하한이 없어 '추천해요' 취소가 등록보다 많이 들어오면
-- 카운트가 음수가 됐다. 프로덕션에 recommend_count = -7 인 행이 실제로 있었다.
--
-- 응답 DTO(CocktailResponseDto.clampNonNegative)가 표시만 0 으로 깎아 왔기 때문에
-- 화면에는 "추천 0" 으로 보이면서 정렬에서는 진짜 0 인 칵테일들보다 뒤로 밀렸다.
-- 인기순 정렬이 붙으면서 이 어긋남이 처음 사용자 눈에 드러난다.
--
-- 쿼리 쪽 하한(WHERE ... > 0)은 같은 커밋에서 막았고, 여기서는 이미 쌓인 값을 되돌린다.
UPDATE cocktail SET recommend_count = 0 WHERE recommend_count < 0;
UPDATE cocktail SET hard_count = 0 WHERE hard_count < 0;
