package com._hateam.auth.application.service;

import com._hateam.auth.application.dto.*;
import com._hateam.auth.domain.TokenInfo;
import com._hateam.auth.domain.model.RefreshToken;
import com._hateam.auth.infrastructure.config.JwtUtil;
import com._hateam.auth.infrastructure.feign.UserClient;
import com._hateam.auth.infrastructure.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserClient userClient;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public ResponseDto<UserSignInResDto> authenticate(UserSignInReqDto userSignInReqDto) {
        // 1. Feign 클라이언트로 사용자 정보 조회
        ResponseEntity<ResponseDto<FeignVerifyResDto>> response = userClient.findByUsername(userSignInReqDto.getUsername());

        // 2. 응답 검증
        if (response.getBody() == null || response.getBody().getData() == null) {
            throw new RuntimeException("사용자 정보를 찾을 수 없습니다.");
        }

        FeignVerifyResDto userData = response.getBody().getData();

        // 3. 비밀번호 검증
        if (!passwordEncoder.matches(userSignInReqDto.getPassword(), userData.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        // 4. JWT 토큰 생성 및 Redis 저장
        TokenInfo tokenInfo = generateTokenAndSaveRefresh(
                userData.getUserId(),
                userData.getUserRole().name()
        );

        // 5. 응답 데이터 생성 (정적 팩토리 메서드 사용)
        UserSignInResDto userSignInResDto = UserSignInResDto.from(
                userData.getUserId(),
              //userData.getUsername(),
                userData.getUserRole().name(),
                tokenInfo
        );


        return ResponseDto.success(userSignInResDto);
    }

    public ResponseDto<UserSignInResDto> refreshToken(String accessToken, String refreshToken) {
        try {
            // 1. 리프레시 토큰이 유효한지 JWT 서명 검증
            if (!jwtUtil.validateToken(refreshToken)) {
                throw new RuntimeException("유효하지 않은 리프레시 토큰입니다.");
            }

            // 2. Redis에서 리프레시 토큰 조회
            java.util.Optional<RefreshToken> storedToken = refreshTokenRepository.findById(refreshToken);
            if (storedToken.isEmpty()) {
                throw new RuntimeException("저장된 리프레시 토큰이 없습니다.");
            }

            RefreshToken refreshTokenEntity = storedToken.get();
            Long userIdFromRefresh = refreshTokenEntity.getUserId();

            // 3. 액세스 토큰에서 사용자 ID 추출 시도 (오류 무시하지 않음)
            Long userIdFromAccess;
            try {
                userIdFromAccess = jwtUtil.extractUserIdWithoutValidation(accessToken);
            } catch (Exception e) {
                throw new RuntimeException("잘못된 액세스 토큰입니다: " + e.getMessage());
            }

            // 4. 두 토큰의 사용자 ID가 일치하는지 확인
            if (!userIdFromRefresh.equals(userIdFromAccess)) {
                throw new RuntimeException("토큰 정보가 일치하지 않습니다. 액세스 토큰과 리프레시 토큰이 동일한 사용자의 것이 아닙니다.");
            }

            // 5. 새 토큰 발급 및 Redis 업데이트
            TokenInfo newTokenInfo = generateTokenAndSaveRefresh(
                    userIdFromRefresh,
                    refreshTokenEntity.getRole()
            );

            // 6. 응답 생성
            UserSignInResDto userSignInResDto = UserSignInResDto.from(
                    userIdFromRefresh,
                    refreshTokenEntity.getRole(),
                    newTokenInfo
            );

            return ResponseDto.success(userSignInResDto);
        } catch (Exception e) {
            throw new RuntimeException("토큰 갱신 중 오류 발생: " + e.getMessage());
        }
    }


    // 토큰으로 로그아웃 처리 메서드
    public ResponseDto<Void> logoutWithToken(String authHeader) {
        try {
            // "Bearer " 제거
            String token = authHeader.substring(7);
            // 토큰에서 사용자 ID 추출
            Long userId = jwtUtil.extractUserId(token);

            // Redis에서 해당 사용자의 모든 리프레시 토큰 삭제
            deleteAllUserTokens(userId);

            return ResponseDto.success(null);
        } catch (Exception e) {
            throw new RuntimeException("로그아웃 처리 중 오류 발생: " + e.getMessage());
        }
    }

    // 토큰 생성 및 Redis 저장 메서드 (수정됨)
    private TokenInfo generateTokenAndSaveRefresh(Long userId, String role) {
        // 토큰 생성
        TokenInfo tokenInfo = jwtUtil.generateToken(userId, role);
        log.info("생성된 리프레시 토큰: {}", tokenInfo.getRefreshToken());

        try {
            // 해당 사용자의 토큰만 삭제 (다른 사용자 영향 없음)
            Optional<RefreshToken> existingToken = refreshTokenRepository.findByUserId(userId);
            if (existingToken.isPresent()) {
                log.info("기존 토큰 삭제: {}", existingToken.get().getId());
                refreshTokenRepository.delete(existingToken.get());
            }

            // 새 토큰 저장
            RefreshToken refreshToken = RefreshToken.of(
                    tokenInfo.getRefreshToken(),
                    userId,
                    role
            );

            RefreshToken saved = refreshTokenRepository.save(refreshToken);
            log.info("Redis에 저장된 토큰: {}", saved.getId());
        } catch (Exception e) {
            log.error("Redis 저장 중 오류: {}", e.getMessage(), e);
            // 저장에 실패해도 토큰은 반환 (인증 과정은 계속 진행)
        }

        return tokenInfo;
    }

// 로그아웃 시 사용자 토큰 삭제 메서드 (수정됨)
public ResponseDto<Void> deleteAllUserTokens(Long userId) {
    try {
        // 해당 사용자의 토큰만 찾아서 삭제
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByUserId(userId);
        if (tokenOpt.isPresent()) {
            refreshTokenRepository.delete(tokenOpt.get());
            log.info("사용자 ID {}의 토큰 삭제 완료", userId);
        } else {
            log.info("사용자 ID {}의 토큰이 존재하지 않습니다", userId);
        }

        return ResponseDto.success(null);
    } catch (Exception e) {
        log.error("사용자 토큰 삭제 중 오류: {}", e.getMessage(), e);
        // 오류가 발생해도 정상 응답 반환
        return ResponseDto.success(null);
    }
}




    }

