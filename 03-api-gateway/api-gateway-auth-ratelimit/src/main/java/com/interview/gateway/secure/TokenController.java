package com.interview.gateway.secure;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Demo-only token issuer on the gateway.
 * Production: Auth Server / Cognito / Keycloak / company IdP.
 */
@RestController
@RequestMapping("/auth")
public class TokenController {

    private final JwtService jwtService;

    public TokenController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public record TokenRequest(String username) {
    }

    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> token(@RequestBody TokenRequest request) {
        if (request == null || request.username() == null || request.username().isBlank()) {
            throw new IllegalArgumentException("username is required");
        }
        String jwt = jwtService.issueToken(request.username().trim());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("accessToken", jwt);
        body.put("tokenType", "Bearer");
        body.put("username", request.username().trim());
        body.put("hint", "Call /api/** with header: Authorization: Bearer <accessToken>");
        return body;
    }
}
