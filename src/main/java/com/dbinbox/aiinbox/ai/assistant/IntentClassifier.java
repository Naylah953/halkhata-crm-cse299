package com.dbinbox.aiinbox.ai.assistant;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;


@Service
public class IntentClassifier
{
    private final ChatClient chatClient;

    public IntentClassifier(ChatClient.Builder builder)
    {
        this.chatClient = builder.build();
    }

    public String classify(String text) {
        return chatClient.prompt()
                .system("Classify the message into: 'PRODUCT_QUERY' (asking about items), 'ORDER_REQUEST' (wants to buy/purchase/order), or 'OTHER'. Reply with ONLY the word.")
                .user(text)
                .call()
                .content().toUpperCase().trim();
    }
}