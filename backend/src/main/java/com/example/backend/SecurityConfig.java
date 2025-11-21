package com.example.backend;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    private static final Logger LOG = LoggerFactory.getLogger(SecurityConfig.class);
    
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;
    
    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Bean
    public JwtDecoder jwtDecoder() {
        LOG.info("=== Configuring JWT Decoder ===");
        LOG.info("Expected Issuer URI: {}", issuerUri);
        LOG.info("JWK Set URI: {}", jwkSetUri);
        
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        
        // Create a custom validator that logs what's happening
        decoder.setJwtValidator(token -> {
            String actualIssuer = token.getIssuer() != null ? token.getIssuer().toString() : "NULL";
            LOG.info("=== JWT Validation ===");
            LOG.info("Token Issuer: {}", actualIssuer);
            LOG.info("Expected Issuer: {}", issuerUri);
            LOG.info("Match: {}", actualIssuer.equals(issuerUri));
            
            // Use Spring's default validators
            return JwtValidators.createDefaultWithIssuer(issuerUri).validate(token);
        });
        
        return decoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Allow unauthenticated access to actuator health endpoint for Docker healthchecks
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            // Configure as OAuth2 Resource Server with JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );
        
        return http.build();
    }

    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();
        
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Get default scopes
            Collection<GrantedAuthority> authorities = defaultConverter.convert(jwt);
            
            // Extract realm roles from realm_access.roles
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            Collection<String> realmRoles = realmAccess != null && realmAccess.get("roles") instanceof Collection
                ? (Collection<String>) realmAccess.get("roles")
                : Collections.emptyList();
            
            Collection<GrantedAuthority> realmAuthorities = realmRoles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
            
            return Stream.concat(authorities.stream(), realmAuthorities.stream())
                .collect(Collectors.toList());
        });
        return converter;
    }
}