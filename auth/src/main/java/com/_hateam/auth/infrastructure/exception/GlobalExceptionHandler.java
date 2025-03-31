package com._hateam.auth.infrastructure.exception;

import com._hateam.auth.application.dto.ResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ResponseDto<Object>> handleRuntimeException(RuntimeException ex) {
        // 400 Bad Request
        ResponseDto<Object> errorResponse = ResponseDto.failure(HttpStatus.BAD_REQUEST, ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // 특정 예외 처리 - 인증 관련 예외 (401 상태 코드 반환)
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ResponseDto<Object>> handleAuthException(AuthException ex) {
        ResponseDto<Object> errorResponse = ResponseDto.failure(HttpStatus.UNAUTHORIZED, ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    // 일반적인 서버 오류 (500 상태 코드 반환)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDto<Object>> handleGenericException(Exception ex) {
        ResponseDto<Object> errorResponse = ResponseDto.failure(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}