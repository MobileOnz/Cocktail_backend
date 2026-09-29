package com.application.domain.cocktail.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.application.common.auth.dto.oauth2Dto.CustomOAuth2User;
import com.application.domain.cocktail.dto.request.BookmarkBatchRequest;
import com.application.domain.cocktail.service.CocktailArchiveService;
import com.application.domain.cocktail.service.CocktailBookmarkService;
import com.application.domain.member.entity.Member;
import com.application.domain.member.service.MemberService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

@DisplayName("북마크 요청 개인정보 로그")
class CocktailBookmarkControllerLoggingTest {

    @Test
    @DisplayName("회원은 memberId로만 식별하고 이름·이메일·전화번호는 로그에 남기지 않는다")
    void logsMemberIdWithoutPersonalInformation() {
        // given: Service는 Mockito로 대체하고 Member도 메모리의 가짜 데이터만 사용한다.
        CocktailBookmarkService bookmarkService = mock(CocktailBookmarkService.class);
        MemberService memberService = mock(MemberService.class);
        CocktailArchiveService archiveService = mock(CocktailArchiveService.class);
        CustomOAuth2User user = mock(CustomOAuth2User.class);
        CocktailBookmarkController controller =
                new CocktailBookmarkController(bookmarkService, memberService, archiveService);

        String credentialId = "google-123456789";
        Member member = new Member();
        member.setId(12345L);
        member.setCredentialId(credentialId);
        member.setName("홍길동");
        member.setNickname("칵테일박사");
        member.setEmail("tester@example.com");
        member.setPhone("010-1234-5678");
        List<Long> cocktailIds = List.of(1L, 2L, 3L);

        when(user.getCredentialId()).thenReturn(credentialId);
        when(memberService.getMemberByCredentialId(credentialId)).thenReturn(member);
        when(bookmarkService.toggleBookmarkBatch(12345L, cocktailIds)).thenReturn(cocktailIds);

        Logger logger = (Logger) LoggerFactory.getLogger(CocktailBookmarkController.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        // when
        try {
            controller.toggleBookmarkBatch(new BookmarkBatchRequest(cocktailIds), user);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        // then
        assertThat(appender.list).hasSize(1);
        String message = appender.list.get(0).getFormattedMessage();
        assertThat(message)
                .contains(
                        "credentialId=go***89",
                        "memberId=12345",
                        "cocktailIds=[1, 2, 3]",
                        "cocktailCount=3")
                .doesNotContain(
                        credentialId,
                        "memberName=",
                        "nickname=",
                        "email=",
                        "phone=",
                        "홍길동",
                        "칵테일박사",
                        "tester@example.com",
                        "010-1234-5678");
    }
}
