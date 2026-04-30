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

    // THE FIX: We inject ProductTool here and attach it to the builder
    // so the AI ALWAYS has access to ALL tools (lookup, draft, requestHuman)
    public CustomerAssistantService(OpenAiChatModel openRouterModel, ProductTool productTool) {
        this.chatClient = ChatClient.builder(openRouterModel)
                .defaultTools(productTool) // <-- Full toolbox permanently attached!
                .build();
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

        // --- THE AUTONOMOUS AGENT INSTRUCTION ---
        String systemInstruction = """
                You are a helpful customer support AI for a shop in Bangladesh.

                [CONTEXT]
                Current Contact ID: {contactId}
                Current Tenant ID: {tenantId}

                [CHAT HISTORY]
                {history}

                [INSTRUCTIONS]
                - Be polite and concise.
                - If the user asks for a price or product, ALWAYS use the 'productLookup' tool first to check our real-time stock and get the correct Product ID.
                - If they want to buy, you MUST ask for their real phone number and delivery address first.
                - CRITICAL GUARDRAIL: DO NOT call the 'draftOrder' tool until the user has explicitly typed out their actual phone number and address in the chat. Never invent data or use placeholders.
                - Once you have gathered their real phone, address, and the exact items they want, use the 'draftOrder' tool.
                - If the user asks to speak to a human or agent, immediately call the 'requestHuman' tool.
                - ALWAYS pass the exact {contactId} and {tenantId} provided above when calling any tool.
                """;

        try {
            return chatClient.prompt()
                    .system(s -> s.text(systemInstruction)
                            .param("contactId", contact.getId())
                            .param("tenantId", safeTenantId)
                            .param("history", historyContext))
                    .user(userText)
                    // .tools(productTool) <-- Removed from here, it's now safely in the default builder above!
                    .advisors(a -> a.param("chat_client_max_tool_calls", 3)) // Allows it to search AND draft in one turn!
                    .call()
                    .content();

        } catch (Exception e) {
            System.err.println("Customer AI Error: " + e.getMessage());
            return "I'm having a little trouble connecting right now. Let me get a human agent to assist you.";
        }
    }
}