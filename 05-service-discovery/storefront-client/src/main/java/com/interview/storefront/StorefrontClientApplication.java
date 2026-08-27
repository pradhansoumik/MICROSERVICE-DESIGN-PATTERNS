package com.interview.storefront;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableDiscoveryClient
public class StorefrontClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(StorefrontClientApplication.class, args);
    }

    /**
     * @LoadBalanced = resolve http://SERVICE-NAME/... via Eureka + pick an instance.
     * Without it, RestTemplate would treat ORDER-SERVICE as a hostname DNS lookup and fail.
     */
    @Bean
    @LoadBalanced
    RestTemplate loadBalancedRestTemplate() {
        return new RestTemplate();
    }
}
