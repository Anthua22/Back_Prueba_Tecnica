package com.example.similarproducts.client;

import java.util.List;
import java.util.Optional;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.example.similarproducts.exception.ProductNotFoundException;
import com.example.similarproducts.model.ProductDetail;

import reactor.core.publisher.Mono;

/** Cliente de las APIs existentes (puerto 3001). */
@Component
public class ProductClient {

    private final WebClient webClient;

    public ProductClient(WebClient existingApiWebClient) {
        this.webClient = existingApiWebClient;
    }

    /** IDs similares ordenados por similitud. Error {@link ProductNotFoundException} si el producto no existe. */
    public Mono<List<String>> getSimilarIds(String productId) {
        return webClient.get()
                .uri("/product/{id}/similarids", productId)
                .retrieve()
                .onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND),
                        response -> Mono.error(new ProductNotFoundException(productId)))
                .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
                .defaultIfEmpty(List.of());
    }

    /**
     * Detalle de un producto. Vacío si no existe (404).
     * Cualquier otro fallo (5xx, timeout...) se propaga como error para que no se cachee.
     */
    public Mono<Optional<ProductDetail>> getProduct(String productId) {
        return webClient.get()
                .uri("/product/{id}", productId)
                .retrieve()
                .bodyToMono(ProductDetail.class)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .onErrorResume(WebClientResponseException.NotFound.class,
                        e -> Mono.just(Optional.empty()));
    }
}
