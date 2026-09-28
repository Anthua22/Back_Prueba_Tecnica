package com.example.similarproducts.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.similarproducts.model.ProductDetail;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    public static final String SIMILAR_PRODUCTS_REF = "#/components/schemas/SimilarProducts";

    @Bean
    public OpenAPI similarProductsOpenApi() {
        Components components = new Components();

        // ProductDetail (generado a partir de la clase y sus anotaciones)
        ModelConverters.getInstance().read(ProductDetail.class)
                .forEach(components::addSchemas);

        // SimilarProducts: array de ProductDetail, tal como define el contrato
        components.addSchemas("SimilarProducts", new ArraySchema()
                .items(new Schema<>().$ref("#/components/schemas/ProductDetail"))
                .minItems(0)
                .uniqueItems(true)
                .description("List of similar products to a given one ordered by similarity"));

        return new OpenAPI()
                .info(new Info()
                        .title("SimilarProducts")
                        .version("1.0")
                        .description("Devuelve los detalles de los productos similares a uno dado."))
                .addServersItem(new Server().url("http://localhost:5000"))
                .components(components);
    }
}
