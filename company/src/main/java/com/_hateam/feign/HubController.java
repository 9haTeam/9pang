package com._hateam.feign;

import com._hateam.common.dto.ResponseDto;
import com._hateam.dto.HubDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "hubs-service")
public interface HubController {

    @GetMapping(value = "/hubs")
    ResponseEntity<ResponseDto<List<HubDto>>> getAllHubs(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "isAsc", defaultValue = "false") boolean isAsc);
    // 업체의 소속 허브 조회
    @GetMapping(value = "/hubs/companies/{id}")
    ResponseEntity<ResponseDto<HubDto>> getHubByCompanyId(@PathVariable UUID id);

    // 상품의 소속 허브 조회
    @GetMapping(value = "/hubs/products/{id}")
    ResponseEntity<ResponseDto<HubDto>> getHubByProductId(@PathVariable UUID id);

    @GetMapping(value = "/hubs/{id}")
    ResponseEntity<ResponseDto<HubDto>> getHub(@PathVariable UUID id);
}
