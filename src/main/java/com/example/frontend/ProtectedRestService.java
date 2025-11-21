package com.example.frontend;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Service for calling backend REST APIs with OAuth2 Bearer token authentication.
 * 
 * Implements resilience patterns:
 * - Circuit Breaker: Stops calling backend if it's down (opens after 50% failure rate)
 * - Retry: Retries failed requests up to 3 times with 2s delay
 * - Time Limiter: Timeout after 10s
 * - Bulkhead: Limits concurrent calls to 25
 * - Fallback: Returns user-friendly error messages
 */
@Service
public class ProtectedRestService {

    private static final Logger log = LoggerFactory.getLogger(ProtectedRestService.class);
    private static final String RESILIENCE_NAME = "backendService";

    private final WebClient webClient;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Value("${backend.url:http://localhost:8082}")
    private String backendUrl;

    @Autowired
    public ProtectedRestService(WebClient.Builder webClientBuilder, OAuth2AuthorizedClientService authorizedClientService) {
        this.webClient = webClientBuilder.build();
        this.authorizedClientService = authorizedClientService;
    }

    @CircuitBreaker(name = RESILIENCE_NAME, fallbackMethod = "fallbackResponse")
    @Retry(name = RESILIENCE_NAME)
    @TimeLimiter(name = RESILIENCE_NAME)
    @Bulkhead(name = RESILIENCE_NAME)
    public Mono<String> callProtectedEndpoint() {
        log.debug("Calling protected endpoint");
        return callBackendService("/api/protected-data");
    }

    @CircuitBreaker(name = RESILIENCE_NAME, fallbackMethod = "fallbackResponse")
    @Retry(name = RESILIENCE_NAME)
    @TimeLimiter(name = RESILIENCE_NAME)
    @Bulkhead(name = RESILIENCE_NAME)
    public Mono<String> callCalendarService() {
        log.debug("Calling calendar service");
        return callBackendService("/api/calendar");
    }

    /**
     * Generic method to call any backend service endpoint with OAuth2 Bearer token.
     * 
     * @param endpoint the API endpoint path (e.g., "/api/calendar")
     * @return Mono containing the response body as String
     * @throws RuntimeException if user is not authenticated or OAuth2 client not found
     */
    private Mono<String> callBackendService(String endpoint) {
        try {
            Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication();
                
            if (!(authentication instanceof OAuth2AuthenticationToken)) {
                log.error("User not authenticated with OAuth2");
                return Mono.error(new RuntimeException("User not authenticated with OAuth2"));
            }

            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName()
            );

            if (client == null) {
                log.error("OAuth2AuthorizedClient not found for user: {}", oauthToken.getName());
                return Mono.error(new RuntimeException("OAuth2AuthorizedClient not found. User may need to re-login."));
            }

            String accessToken = client.getAccessToken().getTokenValue();
            String url = buildUrl(endpoint);
            
            log.debug("Calling backend: {}", url);

            return webClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .doOnSuccess(response -> log.debug("Backend call successful: {}", endpoint))
                .doOnError(error -> log.error("Backend call failed: {} - {}", endpoint, error.getMessage()));
                
        } catch (Exception e) {
            log.error("Unexpected error in callBackendService: {}", e.getMessage(), e);
            return Mono.error(e);
        }
    }

    /**
     * Fallback method called when circuit breaker is OPEN (backend is down).
     * Provides graceful degradation with user-friendly error messages.
     * 
     * @param throwable the exception that triggered the fallback
     * @return Mono with fallback error message
     */
    private Mono<String> fallbackResponse(Throwable throwable) {
        log.warn("Circuit breaker fallback triggered. Backend service unavailable: {}", throwable.getMessage());
        
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException ex = (WebClientResponseException) throwable;
            log.error("Backend returned error: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            return Mono.just("Backend service temporarily unavailable (HTTP " + ex.getStatusCode() + "). Please try again later.");
        }
        
        if (throwable.getCause() instanceof java.net.ConnectException) {
            return Mono.just("⚠️ Backend service is currently down. Our team has been notified. Please try again in a few minutes.");
        }
        
        if (throwable instanceof java.util.concurrent.TimeoutException) {
            return Mono.just("⏱️ Request timed out. The backend service is taking too long to respond. Please try again.");
        }
        
        return Mono.just("⚠️ Service temporarily unavailable. We're working on it. Please try again shortly.");
    }

    /**
     * Builds the full URL by appending endpoint to backend base URL.
     */
    private String buildUrl(String endpoint) {
        if (backendUrl == null || backendUrl.isEmpty()) {
            throw new IllegalStateException("backend.url is not configured");
        }
        if (backendUrl.endsWith("/")) {
            return backendUrl + endpoint.replaceFirst("^/", "");
        }
        return backendUrl + endpoint;
    }
}
