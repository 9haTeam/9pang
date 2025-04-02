package com._hateam.order.infrastructure.config;

import feign.Client;
import feign.Logger;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import feign.httpclient.ApacheHttpClient;

@Configuration
@Import(FeignClientsConfiguration.class)
public class FeignConfig {

    @Value("${feign.client.config.default.connectTimeout:5000}")
    private int connectTimeout;

    @Value("${feign.client.config.default.readTimeout:5000}")
    private int readTimeout;

    @Bean
    public Client feignClient() {
        // Apache HttpClient 설정
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectTimeout)
                .setSocketTimeout(readTimeout)
                .build();

        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .disableCookieManagement()
                .build();

        return new ApacheHttpClient(httpClient);
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}