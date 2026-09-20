-- V17: 비로그인 문의의 기기 식별자 컬럼.
--
-- 앱의 1:1 문의는 로그인하지 않아도 보낼 수 있고, 그 경우 device_number 를 함께 보낸다.
-- 받을 자리가 없으면 담당자가 "누가 보낸 문의인지" 를 전혀 알 수 없다(연락처도 선택 입력이다).
ALTER TABLE inquiry ADD COLUMN IF NOT EXISTS device_number VARCHAR(100);
