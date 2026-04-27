package com.dbinbox.aiinbox.ai.assistant;


import com.dbinbox.aiinbox.ai.tools.CustomerTools;
import com.dbinbox.aiinbox.ai.tools.ProductTool;
import com.dbinbox.aiinbox.model.Conversation;
import com.dbinbox.aiinbox.model.Message;
import com.dbinbox.aiinbox.repository.MessageRepo;
import com.dbinbox.aiinbox.service.OutboundMessageService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CustomerAssistantService {
    private final ChatClient chatClient;

    @Autowired
    private OutboundMessageService outboundMessageService;
    @Autowired
    private MessageRepo messageRepo;
    @Autowired
    private ProductTool productTool;

    // 1. CLEAN CONSTRUCTOR: Remove .defaultTools()
    public CustomerAssistantService(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("You are an assistant for a bakery. Use tools to look up products.")
                // REMOVED .defaultTools(productTool) from here
                .build();
    }

    public void handleAiLogic(Conversation conversation, String userText) {
        List<Message> history = messageRepo.findTop4ByConversationOrderByCreatedAtDesc(conversation);
        Collections.reverse(history);
        String currentContactId = conversation.getContact().getId();

        String historyContext = history.stream()
                .map(m -> m.getContact().getId() + ": " + m.getContent()) // Fixed to use sender/contact name correctly
                .collect(Collectors.joining("\n"));

        System.out.println("--- DEBUG: MANUALLY PREPARED CONTEXT ---\n" + historyContext + "\n---");

        try {
            String response = chatClient.prompt()
                                .system(s -> s.text("""
                You are a bakery assistant for a shop in Bangladesh.
                
                [CUSTOMER_INFO]
                Current Contact ID: {id}
                
                [CHAT_HISTORY]
                {history}
                
                [INSTRUCTIONS]
                - If the user asks for a price, use productLookup.
                - If they want to buy, use draftOrder with the ID provided above.
                            """)
                            .param("id", currentContactId)
                            .param("history", historyContext))
                    .user(userText)
                    // 2. ONLY REGISTER TOOLS HERE
                    .tools(productTool)
                    .call()
                    .content();

            System.out.println("DEBUG: AI Success -> " + response);
            // Save and send logic here...
        } catch (Exception e) {
            System.err.println("AI HANG/ERROR: " + e.getMessage());
        }
    }
}