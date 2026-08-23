package com.application.domain.bar.exception;

import com.application.common.Constant;
import com.application.common.response.ResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * bar 도메인 전용 예외 advice.
 *
 * 전역 CustomExceptionHandler 의 @ExceptionHandler(RuntimeException.class) 가
 * BarAccessException 을 삼켜 500 으로 만들지 않도록 **최고 우선순위**로 먼저 가로챈다.
 * (공유 파일을 수정하지 않기 위한 선택이다.)
 *
 * 응답: { code:-1, msg:"한국어", data:{ reason:"OUT_OF_RANGE", distanceM:1840.2 } }
 * 앱은 문구가 아니라 data.reason 으로 분기한다.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class BarExceptionHandler {

    @ExceptionHandler(BarAccessException.class)
    public ResponseEntity<?> handle(BarAccessException ex) {
        log.info("BarAccessException {} {} : {}", ex.getStatus().value(), ex.getReason(), ex.getMessage());

        Map<String, Object> data = new HashMap<>();
        data.put("reason", ex.getReason());
        if (ex.getDetail() instanceof BarAccessException.Detail d && d.distanceM() != null) {
            data.put("distanceM", d.distanceM());
        }
        return new ResponseEntity<>(
                new ResponseDto<>(Constant.ERROR_CODE, ex.getMessage(), data), ex.getStatus());
    }
}
