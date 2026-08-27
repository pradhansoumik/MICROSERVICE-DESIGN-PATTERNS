package com.interview.storefront;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/storefront")
public class StorefrontController {

    private final RestTemplate restTemplate;

    public StorefrontController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Calls by logical service name — no hardcoded host:port.
     * Load balancer picks a healthy Eureka instance.
     */
    @GetMapping("/summary")
    public Map<String, Object> summary() {
        Map<?, ?> orders = restTemplate.getForObject("http://ORDER-SERVICE/orders", Map.class);
        Map<?, ?> products = restTemplate.getForObject("http://PRODUCT-SERVICE/products", Map.class);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("calledVia", "Eureka service names + @LoadBalanced RestTemplate");
        body.put("orders", orders);
        body.put("products", products);
        return body;
    }

    /** Hit repeatedly with 2 PRODUCT-SERVICE instances to see instancePort flip. */
    @GetMapping("/products")
    public Map<?, ?> productsOnly() {
        return restTemplate.getForObject("http://PRODUCT-SERVICE/products", Map.class);
    }
}
