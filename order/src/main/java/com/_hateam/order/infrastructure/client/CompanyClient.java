package com._hateam.order.infrastructure.client;

import com._hateam.common.dto.ResponseDto;
import com._hateam.order.infrastructure.client.dto.CompanyDto;
import com._hateam.order.infrastructure.client.dto.ProductDto;
import com._hateam.order.infrastructure.client.dto.ProductRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "company-service", url = "${services.company.url}")
public interface CompanyClient {
    
    @GetMapping("/companies/{companyId}")
    ResponseDto<CompanyDto> getCompanyById(@PathVariable("companyId") UUID companyId);

    @GetMapping("/products/{productId}")
    ResponseDto<ProductDto> getProductById(@PathVariable("productId") UUID productId);

    @PatchMapping("/products/{productId}")
    ResponseDto<ProductDto> updateProduct(
            @PathVariable("productId") UUID productId,
            @RequestBody ProductRequestDto requestDto);
}