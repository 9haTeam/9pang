package com._hateam.order.infrastructure.config;

import com._hateam.common.config.BaseSwaggerConfig;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig extends BaseSwaggerConfig {

    @Bean
    @Override
    public OpenAPI baseOpenAPI() {
        OpenAPI baseApi = super.baseOpenAPI();

        return baseApi
                .info(new Info()
                        .title("주문 서비스 API")
                        .description("MSA 기반 물류 관리 및 배송 시스템의 주문 서비스 API입니다.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("9해조")
                                .url("https://github.com/9haTeam"))
                        .license(new License()
                                .name("Creative Commons Attribution-NonCommercial 4.0 International")
                                .url("https://creativecommons.org/licenses/by-nc/4.0/")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Server"),
                        new Server().url("http://dev.9hateam.com").description("Development Server")
                ));
    }
}