package com._hateam.gateway.infrastructure.exception;

import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@Order(-2) // 높은 우선순위로 설정 (기본 핸들러보다 먼저 실행)
public class GlobalErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {

    public GlobalErrorWebExceptionHandler(ErrorAttributes errorAttributes,
                                          WebProperties.Resources resources,
                                          ApplicationContext applicationContext,
                                          ServerCodecConfigurer serverCodecConfigurer) {
        super(errorAttributes, resources, applicationContext);
        super.setMessageWriters(serverCodecConfigurer.getWriters());
        super.setMessageReaders(serverCodecConfigurer.getReaders());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Throwable error = getError(request);

        // ExpiredJwtException 처리
        if (isExpiredJwtException(error)) {
            return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"message\":\"토큰이 만료되었습니다.\"}");
        }

        // 다른 인증 관련 예외 처리
        if (isAuthenticationException(error)) {
            return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"message\":\"적절하지 않은 토큰입니다.\"}");
        }

        // 기타 예외는 기본 처리
        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"message\":\"서버 오류가 발생했습니다.\"}");
    }

    private boolean isExpiredJwtException(Throwable error) {
        Throwable cause = error;
        while (cause != null) {
            if (cause instanceof ExpiredJwtException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private boolean isAuthenticationException(Throwable error) {
        Throwable cause = error;
        while (cause != null) {
            if (cause.getClass().getName().contains("Authentication")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}