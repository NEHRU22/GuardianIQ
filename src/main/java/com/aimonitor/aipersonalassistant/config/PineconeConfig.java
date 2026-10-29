package com.aimonitor.aipersonalassistant.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Configuration
public class PineconeConfig {

    @Bean
    public RestClient pineconeRestClient(
            @Value("${pinecone.api-key}") String apiKey,
            @Value("${pinecone.index-host}") String indexHost) {

        String host = indexHost.trim();

        if (!host.startsWith("https://") && !host.startsWith("http://")) {
            host = "https://" + host;
        }

        return RestClient.builder()
                .baseUrl(host)
                .defaultHeader("Api-Key", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Bean
    public ApplicationRunner testPineconeConnection(
            RestClient pineconeRestClient) {

        return args -> {
            String stats = pineconeRestClient.post()
                    .uri("/describe_index_stats")
                    .header("X-Pinecone-Api-Version", "2025-10")
                    .body(Map.of())
                    .retrieve()
                    .body(String.class);

            System.out.println("PINECONE CONNECTION SUCCESSFUL");
            System.out.println("Index stats: " + stats);
        };
    }
}