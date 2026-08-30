package com.interview.caller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ProductCallerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductCallerApplication.class, args);
    }
}
