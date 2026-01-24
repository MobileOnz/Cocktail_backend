---
name: 커스텀 이슈 템플릿
about: 개발 작업을 위한 템플릿
title: "[이슈 번호] fix: 칵테일 상세 조회 시 북마크 여부 LAZY 로딩 버그 수정"
labels: 'bug'
assignees: ''

---

[//]: # (feat: 새로운 기능)
[//]: # (refactor: 기존 기능 자체는 변하지 않고 코드 개선 :: 로직만 변경)
[//]: # (chore: 빌드 설정, 패키지 매니저 설정 등, 주석제거 등 분류하기 어려운 자잘한 수정에 대한 커밋)
[//]: # (fix: 버그 수정)
[//]: # (style: 포맷팅, 세미콜론 누락, lint 수정 등)
[//]: # (docs: 문서 추가 또는 수정)
[//]: # (test: 테스트 코드)
[//]: # (hotfix: 급한 수정 사항 반영 시 사용)

## 📌 작업 개요
# 이슈번호
<!-- 어떤 기능/버그를 작업했는지 간단히 설명해주세요 -->
- 로그인한 사용자가 칵테일 상세 조회 시 `is_bookmarked`가 항상 `false`로 반환되는 버그 수정
- LAZY 로딩 문제로 인해 `Cocktail.bookmarks` 컬렉션이 영속성 컨텍스트 종료 후 접근되어 발생한 문제
- 북마크 여부를 직접 조회하는 방식으로 변경하여 성능 개선 및 안정성 확보

### 주요 변경사항

1. **CocktailService.getCocktailV2()**
   - `@Transactional(readOnly = true)` 어노테이션 추가
   - 북마크 여부를 `bookmarkRepository.existsByMemberIdAndCocktailId()` 메서드로 직접 조회
   - `Cocktail.isBookmarkedBy()` 메서드 사용 중단 (LAZY 로딩 회피)

2. **CocktailResponseDto**
   - 새로운 팩토리 메서드 추가: `from(Cocktail, ReactionType, Integer, Integer, boolean)`
   - 북마크 여부를 파라미터로 직접 전달받아 LAZY 로딩 문제 원천 차단

### 기술적 개선사항

- **성능**: bookmarks 컬렉션 전체 로딩 대신 EXISTS 쿼리 1회로 북마크 여부 확인
- **안정성**: LazyInitializationException 위험 제거
- **일관성**: 메서드 전체가 하나의 읽기 전용 트랜잭션으로 실행되어 데이터 일관성 보장

## ✨ 기타 참고 사항
<!-- 리뷰어가 참고해야 할 사항이나, 보완 예정인 내용이 있다면 작성해주세요 -->
- 비로그인 사용자의 경우 `is_bookmarked`는 항상 `false`로 반환
- 기존 `Cocktail.isBookmarkedBy(userId)` 메서드는 남겨두었으나, 서비스 레이어에서는 더 이상 사용하지 않음
- 다른 API에서 동일한 LAZY 로딩 패턴이 있는지 확인 필요 (예: 칵테일 목록 조회는 이미 다른 방식으로 처리 중)

## ✅ PR 체크 리스트
- [x] PR 템플릿에 맞추어 작성했어요.
- [x] PR에 적절한 라벨을 선택했어요.
- [x] 이슈번호가 PR 제목 또는 커밋 메시지에 포함되어 있어요.
- [ ] 변경 내용에 대한 테스트를 진행했어요.
- [ ] application.yml 파일을 수정했다면, Notion에 업로드 및 공유했어요.
- [x] 로컬 서버에서 정상 동작을 확인했어요.
- [x] 불필요한 코드/주석 삭제했어요.
