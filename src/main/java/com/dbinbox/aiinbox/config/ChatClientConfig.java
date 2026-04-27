package com.dbinbox.aiinbox.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    /*@Bean
    public ChatMemory chatMemory() {
        // Fix: Use .builder() instead of 'new'
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(3)
                .build();
    }*/

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        // Fix: Use MessageChatMemoryAdvisor.builder() instead of 'new'
        return builder
                //.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultSystem("You are a helpful assistant for a Bangladeshi shop. You understand Banglish.")
                .build();
    }

    //new bean
    // CLASSIFIER CLIENT (For the fast intent check)
    @Bean
    @Qualifier("intentChatClient")
    public ChatClient intentChatClient(ChatClient.Builder builder) {
        return builder
                //.defaultAdvisors()
                .defaultSystem("Classify intent into: SALES, SUPPORT, ESCALATE, or OTHER. Reply with one word only.")
                .build();
    }
}