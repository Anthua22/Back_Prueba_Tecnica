package com.example.similarproducts.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Product detail")
public record ProductDetail(
        @Schema(description = "Identificador del producto", example = "2", minLength = 1, requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @Schema(description = "Nombre del producto", example = "Dress", minLength = 1, requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @Schema(description = "Precio", example = "19.99", requiredMode = Schema.RequiredMode.REQUIRED)
        double price,
        @Schema(description = "Disponibilidad", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean availability) {
}
