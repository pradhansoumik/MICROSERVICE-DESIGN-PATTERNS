package com.interview.order;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Map<String, Object> myOrders(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Authorized — JWT validated via Keycloak JWKS");
        body.put("subject", jwt.getSubject());
        body.put("preferredUsername", jwt.getClaimAsString("preferred_username"));
        body.put("orders", List.of(
                Map.of("id", "O-1001", "item", "Laptop", "status", "CONFIRMED"),
                Map.of("id", "O-1002", "item", "Mouse", "status", "SHIPPED")
        ));
        return body;
    }
}
