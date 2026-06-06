package com.unplan.unplanserver.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Configuration
@Profile("!prod")
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI(
            @Value("${swagger.server-url}") String serverUrl,
            @Value("${swagger.description}") String description
    ) {
        return new OpenAPI()
                .servers(List.of(
                        new Server()
                                .url(serverUrl)
                                .description(description)
                ))
                .info(new Info()
                        .title("Unplan API")
                        .description("Unplan Server API Documentation")
                        .version("v1.0.0"));
    }
}