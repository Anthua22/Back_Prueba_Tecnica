package com.example.similarproducts.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "existing-api")
public record ExistingApiProperties(String baseUrl,
                                    Duration connectTimeout,
                                    Duration responseTimeout,
                                    int maxConnections,
                                    int concurrencyPerRequest,
                                    Duration cacheTtl,
                                    long cacheMaxSize) {
}
