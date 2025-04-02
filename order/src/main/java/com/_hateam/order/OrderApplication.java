package com._hateam.order;

import com._hateam.common.config.CommonApplication;
import com._hateam.order.infrastructure.config.FeignConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@CommonApplication
@EnableFeignClients(defaultConfiguration = FeignConfig.class)
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}