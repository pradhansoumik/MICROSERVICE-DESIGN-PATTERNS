package com.interview.portal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestClient;

@Controller
public class PortalController {

    private final RestClient.Builder restClientBuilder;
    private final String orderApiBaseUrl;

    public PortalController(RestClient.Builder restClientBuilder,
                            @Value("${order-api.base-url}") String orderApiBaseUrl) {
        this.restClientBuilder = restClientBuilder;
        this.orderApiBaseUrl = orderApiBaseUrl;
    }

    @GetMapping("/")
    public String home(@AuthenticationPrincipal OidcUser user, Model model) {
        model.addAttribute("username", user.getPreferredUsername());
        model.addAttribute("fullName", user.getFullName());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("roles", user.getAuthorities());
        return "home";
    }

    /**
     * Portal calls Order API with the user's access token (Bearer).
     * Order API validates JWT via Keycloak JWKS — then returns data.
     */
    @GetMapping("/my-orders")
    public String myOrders(@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
                           @AuthenticationPrincipal OidcUser user,
                           Model model) {
        String accessToken = client.getAccessToken().getTokenValue();

        Object orders = restClientBuilder.build()
                .get()
                .uri(orderApiBaseUrl + "/api/orders")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(Object.class);

        model.addAttribute("username", user.getPreferredUsername());
        model.addAttribute("orders", orders);
        model.addAttribute("accessTokenPreview", preview(accessToken));
        return "orders";
    }

    private static String preview(String token) {
        if (token == null || token.length() < 20) {
            return "(none)";
        }
        return token.substring(0, 12) + "..." + token.substring(token.length() - 8);
    }
}
