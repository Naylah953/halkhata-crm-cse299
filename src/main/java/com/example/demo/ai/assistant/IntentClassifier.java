package com.example.demo.ai.assistant;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

@Service
public class IntentClassifier {
    private final ChatClient chatClient;

    public IntentClassifier(OpenAiChatModel openRouterModel) {
        this.chatClient = ChatClient.builder(openRouterModel).build();
    }

    public String classify(String text) {
        return chatClient.prompt()
                .system("""
                    Classify the message into exactly ONE of these categories:
                    - 'PRODUCT_QUERY': Customer asking about prices, stock, or items.
                    - 'ORDER_REQUEST': Customer wants to buy, purchase, or start an order. They may name the product specifically or choose from a list given to them, and, when prompted, will provide their phone number and delivery address. 
                    - 'HUMAN_REQUEST': Either customer wants a person, agent, human, or mentions 'intervention' OR they express frustration, mention problems/defects.
                    - 'OTHER': General greetings or unrelated chat.
                    
                    Reply with ONLY the category word.
                    """)
                .user(text)
                .call()
                .content().toUpperCase().trim();
    }
}