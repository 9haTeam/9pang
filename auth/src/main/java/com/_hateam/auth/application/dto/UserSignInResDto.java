package com._hateam.auth.application.dto;

import com._hateam.auth.domain.TokenInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSignInResDto {
    private Long userId;
    // private String username;
    private String role;
    private String accessToken;
    private String refreshToken;

    // 정적 팩토리 메서드
    public static UserSignInResDto from(Long userId,  String role, TokenInfo tokenInfo) {
        return UserSignInResDto.builder()
                .userId(userId)
              //.username(username)
                .role(role)
                .accessToken("Bearer "+tokenInfo.getAccessToken())
                .refreshToken(tokenInfo.getRefreshToken())
                .build();
    }






}
