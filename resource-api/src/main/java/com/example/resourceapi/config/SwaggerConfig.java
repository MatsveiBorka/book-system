package com.example.resourceapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${SWAGGER_API_VERSION}")
    private String SWAGGER_API_VERSION;

    @Bean
    public OpenAPI customOpenAPI() {
        SpringDocUtils.getConfig().addJavaTypeToIgnore(JsonNullable.class);
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(JsonNullable.class);
        SpringDocUtils.getConfig().addResponseTypeToIgnore(JsonNullable.class);
        SpringDocUtils.getConfig().addResponseWrapperToIgnore(JsonNullable.class);

        return new OpenAPI()
                .info(new Info()
                        .title("Resource API")
                        .description("REST API for managing books and resources")
                        .version(SWAGGER_API_VERSION)
                        .contact(new Contact()
                                .name("Book System Team")
                                .email("support@example.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development server")
                ));
    }
}
