package com.example.backend;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ProtectedController {

    @GetMapping("/protected-data")
    @PreAuthorize("hasAuthority('SCOPE_openid')")  // Requires valid OAuth2 token with 'openid' scope
    public String getProtectedData() {
        return "This is protected data from the REST service! Only valid tokens can access this.";
    }

    /**
     * Calendar REST Service Endpoint
     * SECURITY REQUIREMENTS:
     * - Must have valid Keycloak JWT token in Authorization header
     * - Token format: "Bearer <jwt_token>"
     * - Token must be signed by Keycloak and match issuer-uri
     * - Token must not be expired
     * - Token must have 'openid' scope
     * - User must have 'calendar-user' realm role
     * - Without valid token: returns 401 Unauthorized
     * - Without required role: returns 403 Forbidden
     * 
     * @param authentication Spring Security authentication object (auto-injected by Spring)
     * @return Calendar service data with user info and timestamp
     */
    @GetMapping("/calendar")
    @PreAuthorize("hasAuthority('SCOPE_openid') and hasAuthority('ROLE_calendar-user')")  // ✓ Enforces: Token MUST have 'openid' scope AND user must have 'calendar-user' role
    public Map<String, Object> calendar(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        // ✓ If we reach here, token is valid (passed JWT validation + scope check)
        response.put("service", "CALENDAR");
        response.put("message", "Calendar Service Data - Protected by Keycloak Token");
        response.put("user", authentication.getName());  // Username from token
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", "success");
        response.put("token_verified", true);
        
        Map<String, Object> data = new HashMap<>();
        data.put("description", "This endpoint requires a valid Keycloak JWT token");
        data.put("authorities", authentication.getAuthorities().toString());
        data.put("principal_name", authentication.getName());
        response.put("data", data);
        
        return response;
    }
}