package com.example.demo.ai.assistant;

import com.example.demo.ai.tools.ModeratorTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

@Service
public class ModeratorAssistantService {

    private final ChatClient primaryClient;   // OpenRouter
    private final ChatClient fallbackClient;  // Gemini

    public ModeratorAssistantService(
            GoogleGenAiChatModel geminiModel,
            OpenAiChatModel openRouterModel,
            ModeratorTools moderatorTools) {

        // 1. Set OpenRouter as the Primary
        this.primaryClient = ChatClient.builder(openRouterModel)
                .defaultTools(moderatorTools)
                .build();

        // 2. Set Gemini as the Fallback
        this.fallbackClient = ChatClient.builder(geminiModel)
                .defaultTools(moderatorTools)
                .build();
    }

    public String useAssistant(String moderatorPrompt, String currentContactId, Long tenantId) {

        // Safely handle missing tenantId during testing (Fixed to prevent TypeMismatchException)
        String safeTenantId = (tenantId != null) ? String.valueOf(tenantId) : "-1";

        String systemInstruction = """
            You are a CRM Admin Assistant for Halkhata. 
            CONTEXT: You are currently acting on the profile of Contact ID: {contextContactId}.
            The shop owner currently logged in has a Tenant ID of: {currentTenantId}.
            
            RULES:
            1. When calling contact tools (createContact, updateContact, deleteContact), you MUST use "{contextContactId}" for the 'psid' parameter.
            2. When calling EVERY tool (including contact tools AND runDatabaseAnalytics), you MUST use "{currentTenantId}" for the 'tenantId' parameter.
            3. Do not ask the moderator for the PSID or Tenant ID; they are provided in this context.
            """;

        try {
            System.out.println("Manager AI: Attempting conversation with Primary Engine (OpenRouter)...");
            return primaryClient.prompt()
                    .system(s -> s.text(systemInstruction)
                            .param("contextContactId", currentContactId)
                            .param("currentTenantId", safeTenantId))
                    .user(moderatorPrompt)
                    .advisors(a -> a.param("chat_memory_conversation_id", currentContactId).param("chat_client_max_tool_calls", 3))
                    .call()
                    .content();

        } catch (Exception openRouterException) {

            System.out.println("Manager AI: OpenRouter failed (" + openRouterException.getMessage() + "). Pivoting to Fallback Engine (Gemini)...");

            try {
                return fallbackClient.prompt()
                        .system(s -> s.text(systemInstruction)
                                .param("contextContactId", currentContactId)
                                .param("currentTenantId", safeTenantId))
                        .user(moderatorPrompt)
                        .advisors(a -> a.param("chat_memory_conversation_id", currentContactId))
                        .call()
                        .content();

            } catch (Exception geminiException) {
                return "I'm sorry, I am currently offline and cannot execute tools or update the database right now. Please try again later.";
            }
        }
    }
}