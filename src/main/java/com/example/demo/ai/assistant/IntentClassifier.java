package com.example.demo.ai.assistant;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

@Service
public class IntentClassifier {
    private final ChatClient chatClient;

    // THE FIX: Explicitly ask Spring for the Gemini model, not a generic builder
    public IntentClassifier(OpenAiChatModel openRouterModel) {
        this.chatClient = ChatClient.builder(openRouterModel).build();
    }

    public String classify(String text) {
        return chatClient.prompt()
                .system("Classify the message into: 'PRODUCT_QUERY' (asking about items), 'ORDER_REQUEST' (wants to buy/purchase/order), or 'OTHER'. Reply with ONLY the word.")
                .user(text)
                .advisors(a -> a.param("chat_client_max_tool_calls", 3))
                .call()
                .content().toUpperCase().trim();
    }
}