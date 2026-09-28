package com.example.similarproducts.service;

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
import com.github.benmanes.caffeine.cache.Caffeine;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class SimilarProductsService {

    private static final Logger log = LoggerFactory.getLogger(SimilarProductsService.class);

    private final ProductClient client;
    private final int concurrency;

    /**
     * Caché de detalles de producto. Guarda los éxitos y los 404 (Optional vacío);
     * los fallos (5xx, timeouts) no se cachean: Caffeine descarta los futuros fallidos.
     * Además, peticiones concurrentes por el mismo ID comparten una única llamada.
     */
    private final AsyncCache<String, Optional<ProductDetail>> cache;

    public SimilarProductsService(ProductClient client, ExistingApiProperties props) {
        this.client = client;
        this.concurrency = props.concurrencyPerRequest();
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(props.cacheTtl())
                .maximumSize(props.cacheMaxSize())
                .buildAsync();
    }

    public Mono<List<ProductDetail>> getSimilarProducts(String productId) {
        return client.getSimilarIds(productId)
                .flatMapMany(Flux::fromIterable)
                .distinct()
                // En paralelo, pero respetando el orden de similitud original
                .flatMapSequential(this::getProductOrSkip, concurrency)
                .collectList();
    }

    /** Devuelve el producto, o vacío si no existe o falla: un producto roto no debe romper la respuesta. */
    private Mono<ProductDetail> getProductOrSkip(String id) {
        // thenApply(identity) evita que una cancelación aguas abajo cancele el futuro compartido de la caché
        return Mono.fromFuture(() -> cache.get(id, (key, executor) -> client.getProduct(key).toFuture())
                        .thenApply(Function.identity()))
                .flatMap(Mono::justOrEmpty)
                .onErrorResume(e -> {
                    log.warn("Producto similar {} no disponible ({}), se omite", id, e.toString());
                    return Mono.empty();
                });
    }
}
