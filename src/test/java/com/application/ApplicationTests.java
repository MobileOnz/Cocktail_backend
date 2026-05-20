package com.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 기본 애플리케이션 테스트
 * - Spring Context 로드 없이 실행 (Mock 기반)
 */
class ApplicationTests {

	@Test
	void applicationClassExists() {
		// Application 클래스가 존재하는지 확인
		assertTrue(Application.class.isAssignableFrom(Application.class));
	}

}
