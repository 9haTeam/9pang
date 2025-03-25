package com._hateam.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;


@EnableDiscoveryClient
@SpringBootApplication
@EnableWebFluxSecurity
@ComponentScan(basePackages = {"com._hateam"}) // 명시적으로 컴포넌트 스캔 범위 설정
//@ComponentScan(basePackages = {"com._hateam.gateway", "com._hateam.filter"})
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }


}