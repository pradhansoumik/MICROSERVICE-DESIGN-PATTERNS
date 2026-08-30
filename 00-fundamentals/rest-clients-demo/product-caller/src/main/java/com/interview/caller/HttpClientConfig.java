package com.interview.caller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class HttpClientConfig {

    @Value("${provider.base-url}")
    private String providerBaseUrl;

    /** 1) Classic sync client */
    @Bean
    RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .rootUri(providerBaseUrl)
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(3))
                .build();
    }

    /** 2) Modern sync client (Spring 6.1+) */
    @Bean
    RestClient restClient() {
        return RestClient.builder()
                .baseUrl(providerBaseUrl)
                .build();
    }
}
