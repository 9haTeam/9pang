package com._hateam.user.application.exception;


import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CustomExceptionHandler {



    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ExceptionResponse> forbiddenException(CustomException ex) {
        int status = HttpServletResponse.SC_FORBIDDEN;
        ExceptionResponse response = new ExceptionResponse("Exception!", ex.getMessage(), status);
        return ResponseEntity.status(status).body(response);
    }



    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponse> exception(Exception ex) {
        int status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        ExceptionResponse response = new ExceptionResponse("그 외 모든 에러", ex.getMessage(), status);
        return ResponseEntity.status(status).body(response);
    }

}
