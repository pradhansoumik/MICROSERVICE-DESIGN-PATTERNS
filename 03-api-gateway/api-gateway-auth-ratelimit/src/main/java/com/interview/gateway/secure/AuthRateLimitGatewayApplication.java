package com.interview.gateway.secure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthRateLimitGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthRateLimitGatewayApplication.class, args);
    }
}
