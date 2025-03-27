package com._hateam.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class BaseSwaggerConfig {

    @Value("${spring.application.name:Service}")
    private String applicationName;

    @Value("${springdoc.swagger-ui.path:/swagger-ui.html}")
    private String swaggerUiPath;

    @Value("${springdoc.api-docs.path:/v3/api-docs}")
    private String apiDocsPath;

    // 기본 OpenAPI 구성 생성
    @Bean
    public OpenAPI baseOpenAPI() {
        return new OpenAPI()
                .info(createApiInfo())
                .servers(createServerList())
                .components(createSecurityComponents())
                .addSecurityItem(new SecurityRequirement().addList("JWT Auth"));
    }

    // API 정보 생성
    private Info createApiInfo() {
        return new Info()
                .title(applicationName + " API")
                .description("MSA 기반 물류 관리 및 배송 시스템의 " + applicationName + " API 문서입니다.")
                .version("v1.0.0")
                .contact(new Contact()
                        .name("9해조")
                        .url("https://github.com/9haTeam")
                        .email("support@9hateam.com"))
                .license(new License()
                        .name("Creative Commons Attribution-NonCommercial 4.0 International")
                        .url("https://creativecommons.org/licenses/by-nc/4.0/"));
    }

    // API 서버 목록 생성
    private List<Server> createServerList() {
        Server localServer = new Server()
                .url("http://localhost:8080")
                .description("Local Server");

        Server devServer = new Server()
                .url("http://dev.9hateam.com")
                .description("Development Server");

        return Arrays.asList(localServer, devServer);
    }

    // 보안 구성 요소 생성
    private Components createSecurityComponents() {
        return new Components()
                .addSecuritySchemes("JWT Auth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .in(SecurityScheme.In.HEADER)
                        .name("Authorization")
                        .description("JWT 인증 토큰을 입력하세요"));
    }
}