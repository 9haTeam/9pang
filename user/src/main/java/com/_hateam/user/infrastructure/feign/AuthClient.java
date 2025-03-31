package com._hateam.user.infrastructure.feign;



import com._hateam.common.dto.ResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;


@FeignClient(name="auth-service")
public interface AuthClient {
    @PostMapping("/api/auth/users/{userId}/tokens")
    ResponseEntity<ResponseDto<Void>> deleteUserTokens(@PathVariable Long userId);
}
