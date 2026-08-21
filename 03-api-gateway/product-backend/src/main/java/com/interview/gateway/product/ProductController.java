package com.interview.gateway.product;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
public class ProductController {

    @GetMapping
    public Map<String, Object> listProducts(
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @RequestHeader(value = "X-Gateway", required = false) String gateway) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "product-backend");
        body.put("port", 8102);
        body.put("products", List.of(
                Map.of("productId", "SKU-100", "name", "Wireless Mouse", "price", 999.0),
                Map.of("productId", "SKU-200", "name", "USB Keyboard", "price", 1499.0)
        ));
        body.put("receivedRequestId", requestId);
        body.put("receivedFromGateway", gateway);
        return body;
    }

    @GetMapping("/{productId}")
    public Map<String, Object> getProduct(
            @PathVariable String productId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @RequestHeader(value = "X-Gateway", required = false) String gateway) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "product-backend");
        body.put("port", 8102);
        body.put("productId", productId);
        body.put("name", "Demo Product " + productId);
        body.put("price", 999.0);
        body.put("receivedRequestId", requestId);
        body.put("receivedFromGateway", gateway);
        return body;
    }
}
