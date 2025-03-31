package com._hateam.auth.presentation.controller;

import com._hateam.auth.application.dto.*;
import com._hateam.auth.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/auth")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    //로그인
    @PostMapping("/signin")
    public ResponseEntity<ResponseDto<UserSignInResDto>> verifyUser(@RequestBody UserSignInReqDto userSignInReqDto) {
        ResponseDto<UserSignInResDto> response = authService.authenticate(userSignInReqDto);
        return ResponseEntity.ok(response);
    }


    //리프레시 토큰 재발급
    @PostMapping("/refresh")
    public ResponseEntity<ResponseDto<UserSignInResDto>> refreshToken(@RequestBody RefreshReqDto refreshReqDto) {

        String accessToken = refreshReqDto.getAccessToken();
        String refreshToken = refreshReqDto.getRefreshToken();

        System.out.println("액세스 토큰: [" + accessToken + "]");
        System.out.println("리프레시 토큰: [" + refreshToken + "]");

        ResponseDto<UserSignInResDto> response = authService.refreshToken(
                refreshReqDto.getAccessToken(),
                refreshReqDto.getRefreshToken()
        );
        return ResponseEntity.ok(response);
    }

    //로그아웃
    @PostMapping("/logout")
    public ResponseEntity<ResponseDto<Void>> logout(@RequestHeader("Authorization") String authHeader) {
        ResponseDto<Void> response = authService.logoutWithToken(authHeader);
        return ResponseEntity.ok(response);
    }

    //유저삭제시 리프레시토큰도 삭제
    @PostMapping("/users/{userId}/tokens")
    public ResponseEntity<ResponseDto<Void>> deleteUserTokens(@PathVariable Long userId) {
        ResponseDto<Void> response = authService.deleteAllUserTokens(userId);
        return ResponseEntity.ok(response);
    }

}
