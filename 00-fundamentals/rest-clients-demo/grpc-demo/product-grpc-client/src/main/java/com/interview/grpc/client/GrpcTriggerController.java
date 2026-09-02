package com.interview.grpc.client;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Thin HTTP facade so you can trigger gRPC from browser/Postman
 * (same idea as testing Feign via /api/feign/...).
 */
@RestController
@RequestMapping("/api/grpc")
public class GrpcTriggerController {

    private final ProductGrpcCaller caller;

    public GrpcTriggerController(ProductGrpcCaller caller) {
        this.caller = caller;
    }

    @GetMapping("/products")
    public Map<String, Object> list() {
        return caller.listProducts();
    }

    @GetMapping("/products/{id}")
    public Map<String, Object> byId(@PathVariable String id) {
        return caller.getProduct(id);
    }
}
