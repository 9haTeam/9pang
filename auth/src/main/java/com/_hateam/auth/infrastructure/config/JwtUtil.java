package com._hateam.auth.infrastructure.config;


import com._hateam.auth.domain.TokenInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
public class JwtUtil {
    @Value("${spring.jwt.secret}")
    private String secret;

    @Value("${spring.jwt.accessTokenValidityInMilliseconds}")
    private long accessTokenValidity; // 액세스 토큰 유효 시간 (밀리초)

    @Value("${spring.jwt.refreshTokenValidityInMilliseconds}")
    private long refreshTokenValidity; // 리프레시 토큰 유효 시간 (밀리초)

    //서명 키 생성
    private Key getSigningKey() {
        byte[] keyBytes = secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    //토큰에서 사용자 Id 추출
    public Long extractUserId(String token) {
        return Long.valueOf(extractClaim(token, claims -> claims.get("userId").toString()));
    }

    //토큰에서 사용자 역할 추출
    public String extractUserRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    //토큰 만료 시간 추출
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    //토큰에서 클레임 추출
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    //토큰에서 모든 클레임 추출
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    //토큰 만료 확인
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    //토큰 생성(access token, refresh token)
    public TokenInfo generateToken(Long userId, String role) {
        // 액세스 토큰 생성
        String accessToken = createToken(userId, role, accessTokenValidity);

        // 리프레시 토큰 생성
        String refreshToken = createToken(userId, role, refreshTokenValidity);

        return TokenInfo.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    //토큰 생성(로직)
    private String createToken(Long userId, String role, long validity) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + validity))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    //토큰 유효성 검증
    public Boolean validateToken(String token) {
        try {
            // 토큰 파싱 및 만료 검사
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.error("토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }
    public Long extractUserIdWithoutValidation(String token) {
        try {
            // 최신 API 사용
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey()) // secret을 적절한 Key 객체로 변환하는 메서드 사용
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return Long.valueOf(claims.get("userId").toString());
        } catch (ExpiredJwtException e) {
            // 만료 예외가 발생해도 클레임은 추출
            return Long.valueOf(e.getClaims().get("userId").toString());
        }
    }






}
