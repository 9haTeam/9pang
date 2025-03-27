package com._hateam.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true", matchIfMissing = true)
@Import(BaseSwaggerConfig.class)
public class SwaggerAutoConfiguration {

    @Value("${spring.application.name:Service}")
    private String applicationName;

    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI defaultOpenAPI(OpenAPI baseOpenAPI) {
        return baseOpenAPI.info(
                new Info()
                        .title(applicationName + " API")
                        .description("MSA 기반 물류 관리 및 배송 시스템의 " + applicationName + " API 문서입니다.")
                        .version("v1.0.0")
        );
    }
}