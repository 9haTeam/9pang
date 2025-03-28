package com._hateam.aistudio.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiClientConfig {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Bean
    public RequestInterceptor geminiRequestInterceptor() {
        return requestTemplate -> requestTemplate.query("key", geminiApiKey);
    }
}
