package com.interview.caller;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * 3) Declarative HTTP client.
 * url = fixed for this simple demo (no Eureka).
 * In discovery demos you use: @FeignClient(name = "PRODUCT-SERVICE") without url.
 */
@FeignClient(name = "productProvider", url = "${provider.base-url}")
public interface ProductFeignClient {

    @GetMapping("/products")
    Map<String, Object> listProducts();

    @GetMapping("/products/{id}")
    Map<String, Object> getProduct(@PathVariable("id") String id);
}
