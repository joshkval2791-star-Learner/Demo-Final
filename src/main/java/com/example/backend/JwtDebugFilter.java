package com.example.backend;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64;

@Component
public class JwtDebugFilter extends OncePerRequestFilter {
    
    private static final Logger log = LoggerFactory.getLogger(JwtDebugFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                // JWT has 3 parts: header.payload.signature
                String[] parts = token.split("\\.");
                if (parts.length >= 2) {
                    // Decode the payload (second part)
                    String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
                    log.info("=== JWT Token Payload ===");
                    log.info(payload);
                    
                    // Extract issuer
                    if (payload.contains("\"iss\"")) {
                        int issStart = payload.indexOf("\"iss\"");
                        String issPart = payload.substring(issStart, Math.min(payload.length(), issStart + 150));
                        log.info("=== ISSUER IN TOKEN: " + issPart);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to decode JWT for debugging", e);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
