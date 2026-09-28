package com.example.similarproducts.service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.similarproducts.client.ProductClient;
import com.example.similarproducts.config.ExistingApiProperties;
import com.example.similarproducts.model.ProductDetail;
import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class SimilarProductsService {

    private static final Logger log = LoggerFactory.getLogger(SimilarProductsService.class);

    /** Tiempo durante el que se omite un producto que acaba de fallar. */
    private static final Duration FAILURE_TTL = Duration.ofSeconds(5);

    private final ProductClient client;
    private final int concurrency;

    /**
     * Caché de IDs similares por producto. Solo guarda éxitos: los fallos y los 404
     * no se cachean. Peticiones concurrentes por el mismo producto comparten una única llamada.
     */
    private final AsyncCache<String, List<String>> similarIdsCache;

    /**
     * Caché de detalles de producto. Guarda los éxitos y los 404 (Optional vacío);
     * los fallos (5xx, timeouts) no se cachean: Caffeine descarta los futuros fallidos.
     * Además, peticiones concurrentes por el mismo ID comparten una única llamada.
     */
    private final AsyncCache<String, Optional<ProductDetail>> productCache;

    /** IDs que han fallado hace poco: se omiten sin volver a llamar (evita esperar el timeout en cada petición). */
    private final Cache<String, Boolean> recentFailures = Caffeine.newBuilder()
            .expireAfterWrite(FAILURE_TTL)
            .maximumSize(10_000)
            .build();

    public SimilarProductsService(ProductClient client, ExistingApiProperties props) {
        this.client = client;
        this.concurrency = props.concurrencyPerRequest();
        this.similarIdsCache = Caffeine.newBuilder()
                .expireAfterWrite(props.cacheTtl())
                .maximumSize(props.cacheMaxSize())
                .buildAsync();
        this.productCache = Caffeine.newBuilder()
                .expireAfterWrite(props.cacheTtl())
                .maximumSize(props.cacheMaxSize())
                .buildAsync();
    }

    public Mono<List<ProductDetail>> getSimilarProducts(String productId) {
        return getSimilarIds(productId)
                .flatMapMany(Flux::fromIterable)
                .distinct()
                // En paralelo, pero respetando el orden de similitud original
                .flatMapSequential(this::getProductOrSkip, concurrency)
                .collectList();
    }

    private Mono<List<String>> getSimilarIds(String productId) {
        // thenApply(identity) evita que una cancelación aguas abajo cancele el futuro compartido de la caché
        return Mono.fromFuture(() -> similarIdsCache
                .get(productId, (key, executor) -> client.getSimilarIds(key).toFuture())
                .thenApply(Function.identity()));
    }

    /** Devuelve el producto, o vacío si no existe o falla: un producto roto no debe romper la respuesta. */
    private Mono<ProductDetail> getProductOrSkip(String id) {
        if (recentFailures.getIfPresent(id) != null) {
            return Mono.empty();
        }
        return Mono.fromFuture(() -> productCache
                        .get(id, (key, executor) -> client.getProduct(key).toFuture())
                        .thenApply(Function.identity()))
                .flatMap(Mono::justOrEmpty)
                .onErrorResume(e -> {
                    recentFailures.put(id, Boolean.TRUE);
                    log.warn("Producto similar {} no disponible ({}), se omite", id, e.toString());
                    return Mono.empty();
                });
    }
}