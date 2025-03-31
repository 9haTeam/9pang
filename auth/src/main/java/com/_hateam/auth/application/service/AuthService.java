package com._hateam.auth.application.service;

import com._hateam.auth.application.dto.ResponseDto;
import com._hateam.auth.application.dto.UserSignInReqDto;
import com._hateam.auth.application.dto.UserSignInResDto;

public interface AuthService {
    ResponseDto<UserSignInResDto> authenticate(UserSignInReqDto userSignInReqDto);

    ResponseDto<UserSignInResDto> refreshToken(String accessToken, String refreshToken);

    ResponseDto<Void> logoutWithToken(String authHeader);

    ResponseDto<Void> deleteAllUserTokens(Long userId);


}
