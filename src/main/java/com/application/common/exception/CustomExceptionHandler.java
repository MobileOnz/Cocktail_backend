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
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.util.NoSuchElementException;

@RestControllerAdvice
@Slf4j
public class CustomExceptionHandler {

    @ExceptionHandler(IOException.class)
    public ResponseEntity<?> ResponseIoException(IOException ex){
        log.warn("IOException : {}", ex.getMessage());
        return new ResponseEntity<>(new ResponseDto<>(Constant.ERROR_CODE, "입출력 오류 발생", ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({NullPointerException.class, NoSuchElementException.class})
    public ResponseEntity<?> ResponseResourceException(RuntimeException ex){
        log.warn("Resource Exception : {}", ex.getMessage());
        return new ResponseEntity<>(new ResponseDto<>(Constant.ERROR_CODE, "리소스를 찾을 수 없음", ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> ResponseRuntimeException(RuntimeException ex){
        log.warn("RuntimeException : {}", ex.getMessage());
        return new ResponseEntity<>(new ResponseDto<>(Constant.ERROR_CODE, "서버 내부 오류 발생", ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(CustomApiException.class)
    public ResponseEntity<?> ResponseCustomApiException(CustomApiException ex){
        log.warn("CustomApiExcepiton : {}", ex.getMessage());
        return new ResponseEntity<>(new ResponseDto<>(Constant.ERROR_CODE, "잘못된 요청", ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TokenInvalidException.class)
    public ResponseEntity<?> ResponseInvalidToken(TokenInvalidException ex){
        log.warn("Token Expired : {}", ex.getMessage());
        return new ResponseEntity<>(new ResponseDto<>(Constant.ERROR_CODE, "유효하지 않은 토큰입니다.", ex.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({MalformedJwtException.class,ExpiredJwtException.class})
    public ResponseEntity<?> handlerJwtException(Exception ex){
        log.warn("JWT Exception : {}", ex.getMessage());
        return new ResponseEntity<>(new ResponseDto<>(Constant.ERROR_CODE, "유효하지 않은 토큰입니다.", ex.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(CustomValidException.class)
    public ResponseEntity<?> customValidationException(CustomValidException ex){
        log.warn("Validation ex : {}", ex.getMessage());
        return new ResponseEntity<>(new ResponseDto<>(Constant.ERROR_CODE, ex.getMessage(), ex.getErrorMap()), HttpStatus.BAD_REQUEST);
    }
}
