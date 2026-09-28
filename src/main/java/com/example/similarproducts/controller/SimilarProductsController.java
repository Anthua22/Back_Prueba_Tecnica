package com.example.similarproducts.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.example.similarproducts.config.OpenApiConfig;
import com.example.similarproducts.model.ProductDetail;
import com.example.similarproducts.service.SimilarProductsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Mono;

@RestController
@Tag(name = "Similar products")
public class SimilarProductsController {

    private final SimilarProductsService service;

    public SimilarProductsController(SimilarProductsService service) {
        this.service = service;
    }

    @Operation(operationId = "get-product-similar",
            summary = "Similar products",
            description = "Lista de productos similares al dado, ordenada por similitud.")
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(ref = OpenApiConfig.SIMILAR_PRODUCTS_REF)))
    @ApiResponse(responseCode = "404", description = "Product Not found", content = @Content)
    @GetMapping("/product/{productId}/similar")
    public Mono<List<ProductDetail>> getSimilar(
            @Parameter(description = "ID del producto", example = "1") @PathVariable String productId) {
        return service.getSimilarProducts(productId);
    }
}
