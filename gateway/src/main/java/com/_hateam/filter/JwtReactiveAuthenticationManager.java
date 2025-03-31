package com._hateam.filter;

import io.jsonwebtoken.Claims;
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

            // 파싱된 Claims 출력 (디버깅용)
            // log.info("Parsed JWT Claims: {}", claims);

            Long userId2 = claims.get("userId", Long.class);
            String role = claims.get("role", String.class);


            // 디버깅 로그
            // log.info("User ID from token: {}", userId2);
            // log.info("Final role after substring check: {}", role);

            List<SimpleGrantedAuthority> authorities =
                    Collections.singletonList(new SimpleGrantedAuthority(role));
            //임시
            String userId=String.valueOf(userId2);
            Authentication auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);

            //log.info("Created Auth: {}", auth);
            //log.info("Auth Authorities: {}", auth.getAuthorities());


            return Mono.just(auth);
        } catch (Exception e) {
            return Mono.empty(); // 인증 실패 시
        }
    }
}