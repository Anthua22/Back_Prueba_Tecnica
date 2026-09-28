package com.example.similarproducts.controller;

import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.example.similarproducts.exception.ProductNotFoundException;
import com.example.similarproducts.model.ProductDetail;
import com.example.similarproducts.service.SimilarProductsService;

import reactor.core.publisher.Mono;

@WebFluxTest(SimilarProductsController.class)
class SimilarProductsControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    SimilarProductsService service;

    @Test
    void returns200WithProducts() {
        when(service.getSimilarProducts("1"))
                .thenReturn(Mono.just(List.of(new ProductDetail("2", "Dress", 19.99, true))));

        webTestClient.get().uri("/product/1/similar")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id").isEqualTo("2")
                .jsonPath("$[0].name").isEqualTo("Dress")
                .jsonPath("$[0].price").isEqualTo(19.99)
                .jsonPath("$[0].availability").isEqualTo(true);
    }

    @Test
    void returns404WhenProductNotFound() {
        when(service.getSimilarProducts("99")).thenReturn(Mono.error(new ProductNotFoundException("99")));

        webTestClient.get().uri("/product/99/similar")
                .exchange()
                .expectStatus().isNotFound();
    }
}
