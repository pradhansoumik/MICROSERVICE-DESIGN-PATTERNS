package com.interview.caller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CallerController {

    private final RestTemplate restTemplate;
    private final RestClient restClient;
    private final ProductFeignClient feignClient;

    public CallerController(RestTemplate restTemplate,
                            RestClient restClient,
                            ProductFeignClient feignClient) {
        this.restTemplate = restTemplate;
        this.restClient = restClient;
        this.feignClient = feignClient;
    }

    /** Learn first: RestTemplate */
    @GetMapping("/rest-template/products")
    public Map<String, Object> viaRestTemplate() {
        Map<?, ?> provider = restTemplate.getForObject("/products", Map.class);
        return wrap("RestTemplate", provider);
    }

    @GetMapping("/rest-template/products/{id}")
    public Map<String, Object> viaRestTemplateById(@PathVariable String id) {
        Map<?, ?> provider = restTemplate.getForObject("/products/{id}", Map.class, id);
        return wrap("RestTemplate", provider);
    }

    /** Learn second: RestClient */
    @GetMapping("/rest-client/products")
    public Map<String, Object> viaRestClient() {
        Map<?, ?> provider = restClient.get()
                .uri("/products")
                .retrieve()
                .body(Map.class);
        return wrap("RestClient", provider);
    }

    @GetMapping("/rest-client/products/{id}")
    public Map<String, Object> viaRestClientById(@PathVariable String id) {
        Map<?, ?> provider = restClient.get()
                .uri("/products/{id}", id)
                .retrieve()
                .body(Map.class);
        return wrap("RestClient", provider);
    }

    /** Learn third: OpenFeign */
    @GetMapping("/feign/products")
    public Map<String, Object> viaFeign() {
        return wrap("OpenFeign", feignClient.listProducts());
    }

    @GetMapping("/feign/products/{id}")
    public Map<String, Object> viaFeignById(@PathVariable String id) {
        return wrap("OpenFeign", feignClient.getProduct(id));
    }

    private Map<String, Object> wrap(String clientType, Map<?, ?> providerBody) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("calledVia", clientType);
        body.put("fromProvider", providerBody);
        return body;
    }
}
