package com.interview.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Value("${server.port}")
    private int port;

    @GetMapping
    public Map<String, Object> list() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "ORDER-SERVICE");
        body.put("instancePort", port);
        body.put("orders", List.of(
                Map.of("id", "O-1001", "item", "Laptop", "status", "CONFIRMED"),
                Map.of("id", "O-1002", "item", "Mouse", "status", "SHIPPED")
        ));
        return body;
    }
}
