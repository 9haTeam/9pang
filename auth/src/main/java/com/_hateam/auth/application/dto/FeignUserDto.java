package com._hateam.auth.application.dto;

import com._hateam.auth.domain.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeignUserDto {
    // User 정보
    private Long userId;

    private String password;

    private UserRole userRole;


}

