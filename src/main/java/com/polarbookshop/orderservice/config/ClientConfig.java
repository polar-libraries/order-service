package com.polarbookshop.orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ClientConfig {

    @Bean
    WebClient webClient(ClientProperties properties, WebClient.Builder webClientBuilder) {
        return webClientBuilder.baseUrl(properties
                .catalogServiceUri().toString()).build();
    }
}
