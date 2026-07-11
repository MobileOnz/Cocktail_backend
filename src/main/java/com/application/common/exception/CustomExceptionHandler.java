package com.application.common.exception;

import com.application.common.Constant;
import com.application.common.exception.custom.CustomApiException;
import com.application.common.exception.custom.CustomValidException;
import com.application.common.exception.custom.TokenInvalidException;
import com.application.common.response.ResponseDto;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * 전역 예외 핸들러.
 *
 * <p>원칙(audit F-11): 예외의 원문 메시지·스택트레이스·SQL이 클라이언트 응답으로
 * <b>절대</b> 나가지 않는다. 서버 로그에만 전체 내용을 남기고, 응답에는 일반화된
 * 메시지와 추적용 <code>errorId</code>만 담는다. 지원 문의 시 이 id로 로그를 찾는다.</p>
 *
 * <p>예외 구분:
 * <ul>
 *   <li>개발자가 사용자에게 보이려고 던진 예외(CustomApiException, CustomValidException,
 *       Token/JWT 예외)의 메시지는 의도된 한국어 안내이므로 그대로 노출한다. 이들은
 *       SQL·내부 구조를 담지 않는다.</li>
 *   <li>그 외 시스템 예외(NPE, IOException, 미처리 RuntimeException/Exception)는
 *       메시지를 숨기고 일반화한다.</li>
 * </ul>
 * </p>
 */
@RestControllerAdvice
@Slf4j
public class CustomExceptionHandler {

    private static String newErrorId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    // ── 프레임워크 상태 예외: 상태코드는 보존하되 메시지는 일반화 ──────────────
    // NoResourceFoundException(핸들러/정적리소스 없음, 404), 405 등은 ResponseStatusException
    // 계열이며 RuntimeException을 상속한다. 아래 handleRuntime이 이를 삼켜 500으로 만들지
    // 않도록 더 구체적인 이 핸들러로 가로채 원래 상태코드를 유지한다(예: 없는 경로 → 404).
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatus(ResponseStatusException ex) {
        log.warn("ResponseStatusException {} : {}", ex.getStatusCode(), ex.getReason());
        String msg = switch (ex.getStatusCode().value()) {
            case 404 -> "요청하신 경로를 찾을 수 없습니다.";
            case 405 -> "허용되지 않은 요청 방식입니다.";
            default -> "요청을 처리할 수 없습니다.";
        };
        return new ResponseEntity<>(new ResponseDto<>(Constant.ERROR_CODE, msg, null), ex.getStatusCode());
    }

    // 매핑되지 않은 경로 → 404. 스프링은 상황에 따라 NoHandlerFoundException(checked) 또는
    // NoResourceFoundException(정적리소스 fallback)을 던진다. 둘 다 명시적으로 404 처리한다.
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<?> handleNoHandler(Exception ex) {
        log.warn("NoHandlerFound : {}", ex.getMessage());
        return new ResponseEntity<>(
                new ResponseDto<>(Constant.ERROR_CODE, "요청하신 경로를 찾을 수 없습니다.", null), HttpStatus.NOT_FOUND);
    }

    // ── 클라이언트 입력 오류: 4xx + 사용자 메시지 (스택/원문 노출 금지) ──────────
    // audit QA P1-1: @Valid 실패·필수 파라미터 누락·잘못된 메서드·깨진 JSON 이 catch-all(handleAny)로
    // 흘러 500 "서버 내부 오류"로 나가던 문제. 이들은 서버 버그가 아니라 잘못된 요청이므로 400/405 로
    // 되돌리고, 검증 메시지처럼 사용자에게 안전한 안내는 그대로 노출한다(SQL·내부구조 없음).

    // Bean Validation(@Valid @RequestBody) 실패 → 400 + 첫 필드 메시지
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String msg = firstFieldMessage(ex.getBindingResult());
        log.warn("MethodArgumentNotValid : {}", msg);
        return new ResponseEntity<>(new ResponseDto<>(Constant.ERROR_CODE, msg, null), HttpStatus.BAD_REQUEST);
    }

    // @Valid @ModelAttribute 등 폼/쿼리 바인딩 검증 실패 → 400 + 첫 필드 메시지
    @ExceptionHandler(BindException.class)
    public ResponseEntity<?> handleBind(BindException ex) {
        String msg = firstFieldMessage(ex.getBindingResult());
        log.warn("BindException : {}", msg);
        return new ResponseEntity<>(new ResponseDto<>(Constant.ERROR_CODE, msg, null), HttpStatus.BAD_REQUEST);
    }

    // 필수 요청 파라미터 누락 → 400
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<?> handleMissingParam(MissingServletRequestParameterException ex) {
        String msg = "필수 파라미터 '" + ex.getParameterName() + "'가 누락되었습니다.";
        log.warn("MissingServletRequestParameter : {}", ex.getParameterName());
        return new ResponseEntity<>(new ResponseDto<>(Constant.ERROR_CODE, msg, null), HttpStatus.BAD_REQUEST);
    }

    // 파라미터 타입 불일치(예: 숫자 자리에 문자) → 400
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String msg = "파라미터 '" + ex.getName() + "'의 형식이 올바르지 않습니다.";
        log.warn("MethodArgumentTypeMismatch : {}", ex.getName());
        return new ResponseEntity<>(new ResponseDto<>(Constant.ERROR_CODE, msg, null), HttpStatus.BAD_REQUEST);
    }

    // 요청 본문 파싱 실패(깨진 JSON 등) → 400. 원문(파서 위치·클래스)은 로그에만.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleNotReadable(HttpMessageNotReadableException ex) {
        log.warn("HttpMessageNotReadable : {}", ex.getMessage());
        return new ResponseEntity<>(
                new ResponseDto<>(Constant.ERROR_CODE, "요청 본문을 해석할 수 없습니다.", null), HttpStatus.BAD_REQUEST);
    }

    // 허용되지 않은 HTTP 메서드(GET 전용에 POST 등) → 405
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("HttpRequestMethodNotSupported : {}", ex.getMethod());
        return new ResponseEntity<>(
                new ResponseDto<>(Constant.ERROR_CODE, "허용되지 않은 요청 방식입니다.", null),
                HttpStatus.METHOD_NOT_ALLOWED);
    }

    /** 바인딩 결과에서 첫 번째 필드 검증 메시지를 뽑는다. 없으면 일반 안내. */
    private String firstFieldMessage(org.springframework.validation.BindingResult br) {
        FieldError fe = br.getFieldError();
        if (fe != null && fe.getDefaultMessage() != null) {
            return fe.getDefaultMessage();
        }
        return "입력값이 올바르지 않습니다.";
    }

    // ── 시스템 예외: 메시지 숨김 + errorId만 반환 ─────────────────────────────

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<?> handleNotFound(NoSuchElementException ex) {
        String errorId = newErrorId();
        log.warn("[{}] NoSuchElement", errorId, ex);
        return new ResponseEntity<>(
                new ResponseDto<>(Constant.ERROR_CODE, "요청하신 리소스를 찾을 수 없습니다.", errorId),
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<?> handleIo(IOException ex) {
        String errorId = newErrorId();
        log.error("[{}] IOException", errorId, ex);
        return new ResponseEntity<>(
                new ResponseDto<>(Constant.ERROR_CODE, "입출력 처리 중 오류가 발생했습니다.", errorId),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<?> handleNpe(NullPointerException ex) {
        String errorId = newErrorId();
        log.error("[{}] NullPointerException", errorId, ex);   // 서버 버그 → 500 (과거 404 위장 금지)
        return new ResponseEntity<>(
                new ResponseDto<>(Constant.ERROR_CODE, "서버 내부 오류가 발생했습니다.", errorId),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntime(RuntimeException ex) {
        String errorId = newErrorId();
        log.error("[{}] Unhandled RuntimeException", errorId, ex);
        return new ResponseEntity<>(
                new ResponseDto<>(Constant.ERROR_CODE, "서버 내부 오류가 발생했습니다.", errorId),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAny(Exception ex) {
        String errorId = newErrorId();
        log.error("[{}] Unhandled Exception", errorId, ex);
        return new ResponseEntity<>(
                new ResponseDto<>(Constant.ERROR_CODE, "서버 내부 오류가 발생했습니다.", errorId),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ── 의도된 사용자 대상 예외: 안내 메시지 노출 (SQL/내부구조 없음) ───────────

    @ExceptionHandler(CustomApiException.class)
    public ResponseEntity<?> handleCustomApi(CustomApiException ex) {
        log.warn("CustomApiException : {}", ex.getMessage());
        return new ResponseEntity<>(
                new ResponseDto<>(Constant.ERROR_CODE, ex.getMessage(), null), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TokenInvalidException.class)
    public ResponseEntity<?> handleInvalidToken(TokenInvalidException ex) {
        log.warn("TokenInvalidException : {}", ex.getMessage());
        return new ResponseEntity<>(
                new ResponseDto<>(Constant.ERROR_CODE, "유효하지 않은 토큰입니다.", null), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({MalformedJwtException.class, ExpiredJwtException.class})
    public ResponseEntity<?> handleJwt(Exception ex) {
        log.warn("JWT Exception : {}", ex.getMessage());
        return new ResponseEntity<>(
                new ResponseDto<>(Constant.ERROR_CODE, "유효하지 않은 토큰입니다.", null), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(CustomValidException.class)
    public ResponseEntity<?> handleValidation(CustomValidException ex) {
        log.warn("Validation Exception : {}", ex.getMessage());
        return new ResponseEntity<>(
                new ResponseDto<>(Constant.ERROR_CODE, ex.getMessage(), ex.getErrorMap()), HttpStatus.BAD_REQUEST);
    }
}
