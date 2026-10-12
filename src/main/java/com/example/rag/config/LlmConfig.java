package com.example.rag.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LlmConfig {

    @Bean
    ChatLanguageModel chatLanguageModel(@Value("${rag.ollama-url}") String baseUrl,
                                        @Value("${rag.model}") String model) {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(model)
                .temperature(0.2)
                .timeout(Duration.ofMinutes(3))
                .build();
    }
}
