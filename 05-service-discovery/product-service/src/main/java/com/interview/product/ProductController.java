package com.interview.product;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
public class ProductController {

    @Value("${server.port}")
    private int port;

    @GetMapping
    public Map<String, Object> list() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "PRODUCT-SERVICE");
        body.put("instancePort", port);
        body.put("hint", "Run a 2nd instance on another port to see load balancing");
        body.put("products", List.of(
                Map.of("id", "P-1", "name", "Laptop"),
                Map.of("id", "P-2", "name", "Mouse")
        ));
        return body;
    }
}
