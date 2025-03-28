//package com._hateam.common.exception;
//
//import com._hateam.common.dto.ResponseDto;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.security.access.AccessDeniedException;
//import org.springframework.security.web.access.AccessDeniedHandler;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//
//@Component
//public class CustomAccessDeniedHandler implements AccessDeniedHandler {
//
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    @Override
//    public void handle(HttpServletRequest request, HttpServletResponse response,
//                       AccessDeniedException e) throws IOException {
//
//        ResponseDto responseDto = ResponseDto.failure(HttpStatus.FORBIDDEN, e.getMessage());
//
//        response.setStatus(403);
//        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
//        response.setCharacterEncoding(StandardCharsets.UTF_8.toString());
//        response.getWriter().write(objectMapper.writeValueAsString(responseDto));
//    }
//}