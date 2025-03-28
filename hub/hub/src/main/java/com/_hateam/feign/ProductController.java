package com._hateam.feign;

import com._hateam.common.dto.ResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;
@FeignClient(
        name = "ProductController"
)
public interface ProductController {
    //    허브의 소속 상품 조회
    @GetMapping(value = "/products/hub/{hubId}")
    ResponseEntity<ResponseDto<ProductDto>> getProductsByHubId(@PathVariable UUID hubId);
}
