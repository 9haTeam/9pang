package com._hateam.feign;

import com._hateam.common.dto.ResponseDto;
import jakarta.ws.rs.core.Application;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "company-service")
public interface CompanyController {
    //    허브의 소속 특정 업체 조회
    @GetMapping("/companies/hub/{hubId}/{companyId}")
    ResponseEntity<ResponseDto<CompanyDto>> getCompanyByCompanyIdAndHubId(
            @PathVariable UUID hubId,
            @PathVariable UUID companyId);

    //    허브의 소속 업체 전체 조회
    @GetMapping(value = "/companies/hub/{hubId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ResponseDto<List<CompanyDto>>> getCompaniesByHubId(
            @PathVariable UUID hubId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "isAsc", defaultValue = "false") boolean isAsc);
}
