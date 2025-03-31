package com._hateam.auth.application.dto;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RefreshReqDto {
    private String accessToken;  // 만료된 액세스 토큰
    private String refreshToken; // 유효한 리프레시 토큰

}
