package com.example.demo.ai.assistant;

import com.example.demo.ai.tools.ProductTool;
import com.example.demo.domain.Contact;
import com.example.demo.domain.Message;
import com.example.demo.repository.MessageRepo;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerAssistantService {
    private final ChatClient chatClient;

    @Autowired
    private MessageRepo messageRepo;

    @Autowired
    private ProductTool productTool;

    // THE FIX: Explicitly ask Spring for the OpenRouter model
    public CustomerAssistantService(OpenAiChatModel openRouterModel) {
        this.chatClient = ChatClient.builder(openRouterModel).build();
    }

    public String handleAiLogic(Contact contact, String userText, Long tenantId) {
        // Prevent parsing crashes if header drops
        String safeTenantId = (tenantId != null) ? String.valueOf(tenantId) : "-1";

        // Map Chat History properly using your Message entity
        List<Message> history = messageRepo.findTop10ByContactOrderByCreatedAtDesc(contact);
        Collections.reverse(history);

        String historyContext = history.stream()
                .map(m -> m.getSenderType() + ": " + m.getContent())
                .collect(Collectors.joining("\n"));

        String systemInstruction = """
                You are a helpful customer support AI for a shop in Bangladesh.
                
                [CONTEXT]
                Current Contact ID: {contactId}
                Current Tenant ID: {tenantId}
                
                [CHAT HISTORY]
                {history}
                
                [INSTRUCTIONS]
                - Be polite and concise.
                - If the user asks for a price or product, use the 'productLookup' tool.
                - If they want to buy, ask for their name, phone number, and delivery address.
                - Once you have their name, phone, address, and the items they want, use the 'draftOrder' tool.
                - ALWAYS pass the exact {contactId} and {tenantId} provided above when calling tools.
                """;

        try {
            return chatClient.prompt()
                    .system(s -> s.text(systemInstruction)
                            .param("contactId", contact.getId())
                            .param("tenantId", safeTenantId)
                            .param("history", historyContext))
                    .user(userText)
                    .tools(productTool)
                    .advisors(a -> a.param("chat_client_max_tool_calls", 3))
                    .call()
                    .content();

        } catch (Exception e) {
            System.err.println("Customer AI Error: " + e.getMessage());
            return "I'm having a little trouble connecting right now. Let me get a human agent to assist you.";
        }
    }
}

