package com.example.similarproducts.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Configuration
@EnableConfigurationProperties(ExistingApiProperties.class)
public class AppConfig {

    /** WebClient no bloqueante con pool de conexiones y timeouts hacia la API existente. */
    @Bean
    public WebClient existingApiWebClient(ExistingApiProperties props) {
        ConnectionProvider pool = ConnectionProvider.builder("existing-api")
                .maxConnections(props.maxConnections())
                .pendingAcquireMaxCount(-1) // cola ilimitada: nunca rechaza por saturación del pool
                .build();

        HttpClient httpClient = HttpClient.create(pool)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) props.connectTimeout().toMillis())
                .responseTimeout(props.responseTimeout());

        return WebClient.builder()
                .baseUrl(props.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
