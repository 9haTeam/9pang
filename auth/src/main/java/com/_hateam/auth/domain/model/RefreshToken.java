package com._hateam.auth.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "refreshToken", timeToLive = 1209600) // 14일(초 단위)
public class RefreshToken {

    @Id
    private String id; // 리프레시 토큰 자체를 ID로 사용

    @Indexed
    private Long userId;
    private String role;
    private LocalDateTime createdAt;

    public static RefreshToken of(String token, Long userId, String role) {
        return RefreshToken.builder()
                .id(token)
                .userId(userId)
                .role(role)
                .createdAt(LocalDateTime.now())
                .build();
    }

}
