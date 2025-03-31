package com._hateam.gateway.infrastructure.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Component
//@Slf4j
public class JwtReactiveAuthenticationManager implements ReactiveAuthenticationManager {
    @Value("${service.jwt.secret-key}")
    private String secretKey;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = authentication.getCredentials().toString();
        try {
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long userId2 = claims.get("userId", Long.class);
            String role = claims.get("role", String.class);

            List<SimpleGrantedAuthority> authorities =
                    Collections.singletonList(new SimpleGrantedAuthority(role));

            String userId=String.valueOf(userId2);
            Authentication auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);



            return Mono.just(auth);

        } catch (ExpiredJwtException e) {  // 토큰 만료 예외 처리


            // JWT 토큰 만료 예외를 특별히 처리하기 위한 커스텀 예외 발생
            return Mono.error(e);//GlobalErrorWebExceptionHandler로 처리(SecurityConfig필터 우회하므로)
        } catch (Exception e) {
            // 다른 모든 예외

            return Mono.empty(); // 인증 실패 시
        }


    }
}