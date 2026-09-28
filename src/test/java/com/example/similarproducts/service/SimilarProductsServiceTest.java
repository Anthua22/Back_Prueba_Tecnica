package com.example.similarproducts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.example.similarproducts.client.ProductClient;
import com.example.similarproducts.config.ExistingApiProperties;
import com.example.similarproducts.exception.ProductNotFoundException;
import com.example.similarproducts.model.ProductDetail;

import reactor.core.publisher.Mono;

class SimilarProductsServiceTest {

    private final ProductClient client = mock(ProductClient.class);
    private final SimilarProductsService service = new SimilarProductsService(client,
            new ExistingApiProperties("http://localhost:3001", Duration.ofSeconds(1),
                    Duration.ofSeconds(2), 100, 8, Duration.ofMinutes(1), 100));

    private final ProductDetail p1 = new ProductDetail("1", "Shirt", 9.99, true);
    private final ProductDetail p3 = new ProductDetail("3", "Boots", 39.99, false);

    @Test
    void keepsSimilarityOrderAndSkipsMissingOrFailingProducts() {
        when(client.getSimilarIds("10")).thenReturn(Mono.just(List.of("3", "2", "4", "1")));
        when(client.getProduct("3")).thenReturn(Mono.just(Optional.of(p3)));
        when(client.getProduct("2")).thenReturn(Mono.just(Optional.empty()));               // 404
        when(client.getProduct("4")).thenReturn(Mono.error(new RuntimeException("500")));   // fallo
        when(client.getProduct("1")).thenReturn(Mono.just(Optional.of(p1)));

        assertThat(service.getSimilarProducts("10").block()).containsExactly(p3, p1);
    }

    @Test
    void propagatesNotFoundWhenBaseProductDoesNotExist() {
        when(client.getSimilarIds("99")).thenReturn(Mono.error(new ProductNotFoundException("99")));

        assertThatThrownBy(() -> service.getSimilarProducts("99").block())
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void returnsEmptyListWhenThereAreNoSimilarProducts() {
        when(client.getSimilarIds("5")).thenReturn(Mono.just(List.of()));

        assertThat(service.getSimilarProducts("5").block()).isEmpty();
    }

    @Test
    void cachesProductDetails() {
        when(client.getSimilarIds("10")).thenReturn(Mono.just(List.of("1")));
        when(client.getProduct("1")).thenReturn(Mono.just(Optional.of(p1)));

        service.getSimilarProducts("10").block();
        service.getSimilarProducts("10").block();

        verify(client, times(1)).getProduct("1");
    }

    @Test
    void skipsRecentlyFailedProductsWithoutCallingAgain() {
        when(client.getSimilarIds("10")).thenReturn(Mono.just(List.of("1")));
        when(client.getProduct("1")).thenReturn(Mono.error(new RuntimeException("timeout")));

        assertThat(service.getSimilarProducts("10").block()).isEmpty();
        assertThat(service.getSimilarProducts("10").block()).isEmpty();

        verify(client, times(1)).getProduct("1");
    }
}
