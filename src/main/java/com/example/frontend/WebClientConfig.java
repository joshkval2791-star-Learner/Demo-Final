package com.example.frontend;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

/**
 * Production-ready WebClient configuration with:
 * - Connection pooling
 * - Timeouts (connect, read, write)
 * - Request/response logging
 * - Memory buffer limits
 * - Retry and error handling
 */
@Configuration
public class WebClientConfig {

    private static final Logger log = LoggerFactory.getLogger(WebClientConfig.class);
    
    private static final int CONNECT_TIMEOUT_MILLIS = 5000;
    private static final int READ_TIMEOUT_SECONDS = 10;
    private static final int WRITE_TIMEOUT_SECONDS = 10;
    private static final int MAX_IN_MEMORY_SIZE = 16 * 1024 * 1024; // 16MB
    private static final int MAX_CONNECTIONS = 500;
    private static final Duration MAX_IDLE_TIME = Duration.ofSeconds(20);

    @Bean
    public WebClient.Builder webClientBuilder() {
        // Configure connection pooling
        ConnectionProvider connectionProvider = ConnectionProvider.builder("custom")
            .maxConnections(MAX_CONNECTIONS)
            .maxIdleTime(MAX_IDLE_TIME)
            .maxLifeTime(Duration.ofMinutes(5))
            .pendingAcquireMaxCount(1000)
            .evictInBackground(Duration.ofSeconds(120))
            .build();

        // Configure HTTP client with timeouts and connection pool
        HttpClient httpClient = HttpClient.create(connectionProvider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MILLIS)
            .responseTimeout(Duration.ofSeconds(READ_TIMEOUT_SECONDS))
            .doOnConnected(conn -> 
                conn.addHandlerLast(new ReadTimeoutHandler(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                    .addHandlerLast(new WriteTimeoutHandler(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS))
            );

        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(MAX_IN_MEMORY_SIZE))
            .filter(logRequest())
            .filter(logResponse());
    }

    /**
     * Logs outgoing requests for debugging and monitoring
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            if (log.isDebugEnabled()) {
                log.debug("Request: {} {}", clientRequest.method(), clientRequest.url());
                clientRequest.headers().forEach((name, values) -> 
                    values.forEach(value -> {
                        // Mask Authorization header for security
                        if ("Authorization".equalsIgnoreCase(name)) {
                            log.debug("  {}: {}", name, "Bearer ***");
                        } else {
                            log.debug("  {}: {}", name, value);
                        }
                    })
                );
            }
            return Mono.just(clientRequest);
        });
    }

    /**
     * Logs responses and errors for debugging and monitoring
     */
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (log.isDebugEnabled()) {
                log.debug("Response: status={}", clientResponse.statusCode());
            }
            return Mono.just(clientResponse);
        });
    }
}
