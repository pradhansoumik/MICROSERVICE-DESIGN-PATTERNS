package com.interview.provider;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
public class ProductController {

    @GetMapping
    public Map<String, Object> list() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("source", "product-provider");
        body.put("products", List.of(
                Map.of("id", "P-1", "name", "Laptop", "price", 75000),
                Map.of("id", "P-2", "name", "Mouse", "price", 500)
        ));
        return body;
    }

    @GetMapping("/{id}")
    public Map<String, Object> byId(@PathVariable String id) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("source", "product-provider");
        body.put("id", id);
        body.put("name", "Laptop");
        body.put("price", 75000);
        return body;
    }
}
