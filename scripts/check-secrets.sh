#!/bin/bash

# 민감 정보 노출 방지 검증 스크립트
# 사용법: ./scripts/check-secrets.sh

set -e

echo "🔍 민감 정보 검증 시작..."

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

ERROR_FOUND=0

# 1. Staged files에서 민감 정보 패턴 검색
echo ""
echo "📋 Step 1: Staged files 검사 중..."
if git diff --cached --name-only | grep -qE "\.(properties|yml|yaml|md)$"; then
    if git diff --cached | grep -iE "=(AKI|AKIA)[A-Z0-9]{16}"; then
        echo -e "${RED}❌ AWS Access Key 발견!${NC}"
        ERROR_FOUND=1
    fi

    if git diff --cached | grep -iE "password=.{8,}(?!\$\{)"; then
        echo -e "${YELLOW}⚠️  실제 비밀번호로 보이는 값 발견 (확인 필요)${NC}"
    fi

    if git diff --cached | grep -iE "secret.*=.{32,}"; then
        echo -e "${YELLOW}⚠️  32자 이상의 secret 값 발견 (확인 필요)${NC}"
    fi
fi

# 2. CLAUDE.md 파일 검사
echo ""
echo "📋 Step 2: CLAUDE.md 검사 중..."
if [ -f "CLAUDE.md" ]; then
    # 코드블록(```)과 EXAMPLE 패턴을 제외하고 검사
    # 실제 AWS Key 패턴 (EXAMPLE이 아닌 경우만)
    if grep -E "(AKI|AKIA)[A-Z0-9]{16}" CLAUDE.md | grep -v "EXAMPLE" | grep -v '```' | grep -qE "(AKI|AKIA)[A-Z0-9]{16}"; then
        echo -e "${RED}❌ CLAUDE.md에 실제 AWS Key 발견!${NC}"
        grep -nE "(AKI|AKIA)[A-Z0-9]{16}" CLAUDE.md | grep -v "EXAMPLE" | grep -v '```' || true
        ERROR_FOUND=1
    fi

    # 실제 Secret Key 패턴 (40자 이상의 영숫자+특수문자, 예시 제외)
    if grep -E "SECRET.*=.*[a-zA-Z0-9+/]{40,}" CLAUDE.md | grep -v "your-super-secret" | grep -v '```' | grep -qE "SECRET"; then
        echo -e "${RED}❌ CLAUDE.md에 실제 Secret Key로 보이는 값 발견!${NC}"
        ERROR_FOUND=1
    fi

    # IP 주소나 실제 도메인 (localhost 제외)
    if grep -E "[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}:[0-9]+" CLAUDE.md | grep -v "127.0.0.1" | grep -v "localhost" | grep -qE "[0-9]+\.[0-9]+"; then
        echo -e "${YELLOW}⚠️  CLAUDE.md에 IP:PORT 발견 (공개 정보인지 확인)${NC}"
    fi
fi

# 3. .gitignore 검증
echo ""
echo "📋 Step 3: .gitignore 검증 중..."
if [ -f ".gitignore" ]; then
    if ! grep -q "application-local" .gitignore; then
        echo -e "${YELLOW}⚠️  .gitignore에 'application-local' 패턴 없음${NC}"
    fi

    if ! grep -q "\.env" .gitignore; then
        echo -e "${YELLOW}⚠️  .gitignore에 '.env' 패턴 없음${NC}"
    fi
else
    echo -e "${RED}❌ .gitignore 파일이 없습니다!${NC}"
    ERROR_FOUND=1
fi

# 4. application.properties 검사
echo ""
echo "📋 Step 4: application.properties 검사 중..."
if [ -f "src/main/resources/application.properties" ]; then
    # 의심스러운 실제 값 패턴
    if grep -E "^[A-Z_]+.*=.{20,}" src/main/resources/application.properties | grep -v "\${" | grep -vE "(url=|REDIRECT_URI=|TOKEN_URL=|PUBLIC_KEY_URL=)" | grep -qE "[A-Z0-9]{20,}"; then
        echo -e "${YELLOW}⚠️  application.properties에 의심스러운 긴 값 발견 (수동 확인 필요)${NC}"
        echo "   다음 라인들을 확인하세요:"
        grep -nE "^[A-Z_]+.*=.{20,}" src/main/resources/application.properties | grep -v "\${" | grep -vE "(url=|REDIRECT_URI=|TOKEN_URL=|PUBLIC_KEY_URL=)" || true
    fi
fi

# 결과 출력
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
if [ $ERROR_FOUND -eq 1 ]; then
    echo -e "${RED}❌ 검증 실패: 민감 정보가 발견되었습니다!${NC}"
    echo "   커밋하기 전에 반드시 제거하세요."
    exit 1
else
    echo -e "${GREEN}✓ 검증 통과: 명백한 민감 정보는 발견되지 않았습니다.${NC}"
    echo ""
    echo "⚠️  주의: 이 스크립트는 완벽하지 않습니다."
    echo "   커밋 전 항상 수동으로도 확인하세요!"
fi

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
