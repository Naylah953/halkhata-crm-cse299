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

        this.primaryClient = ChatClient.builder(openRouterModel)
                .defaultTools(moderatorTools)
                .build();

        this.fallbackClient = ChatClient.builder(geminiModel)
                .defaultTools(moderatorTools)
                .build();
    }

    public String useAssistant(String moderatorPrompt, String currentContactId, Long tenantId, String adminName) {

        // Safely handle missing tenantId during testing
        String safeTenantId = (tenantId != null) ? String.valueOf(tenantId) : "-1";
        String safeAdminName = (adminName != null) ? adminName : "Admin";

        // CRITICAL FIX: Secure a unique memory bucket for Global Queries so different shops don't share memory
        String memoryId = (currentContactId != null && !currentContactId.trim().isEmpty())
                ? currentContactId
                : "global_shop_" + safeTenantId;

        String systemInstruction;

        // Dynamic System Prompt based on SPA Context
        if (currentContactId != null && !currentContactId.trim().isEmpty()) {
            systemInstruction = """
                You are a CRM Admin Assistant for Halkhata. You are assisting {adminName}.
                CONTEXT: You are currently acting on the profile of Contact ID: {contextContactId}.
                The shop owner currently logged in has a Tenant ID of: {currentTenantId}.
                
                RULES:
                1. When calling contact tools (createContact, updateContact, deleteContact), you MUST use "{contextContactId}" for the 'psid' parameter.
                2. When calling EVERY tool (including contact tools AND runDatabaseAnalytics), you MUST use "{currentTenantId}" for the 'tenantId' parameter.
                3. Do not ask the moderator for the PSID or Tenant ID; they are provided in this context.
                4. CONTEXT ISOLATION: Safely drop previous context ONLY when a completely new topic is initiated. During an ongoing multi-turn task (like gathering missing parameters for an update), you MUST remember the intent and parameters from the immediate previous messages. Do not hallucinate IDs.
                5. ZERO-RESULT FALLBACK: If the 'runDatabaseAnalytics' SQL query returns empty data ([]), you must reply exclusively with 'No records found' and offer no further explanation. For all other CRM tools (like manageOrders or handleComplaints), you must naturally relay the tool's text response back to the admin.
                """;
        } else {
            systemInstruction = """
                You are a CRM Admin Assistant for Halkhata. You are assisting {adminName}.
                CONTEXT: You are assisting the admin globally. No specific customer is selected.
                The shop owner currently logged in has a Tenant ID of: {currentTenantId}.
                
                RULES:
                1. Do not use contact-specific tools like updateContact unless the admin provides a PSID in their message.
                2. When calling EVERY tool (including contact tools AND runDatabaseAnalytics), you MUST use "{currentTenantId}" for the 'tenantId' parameter.
                3. Do not ask the moderator for the Tenant ID; it is provided in this context.
                4. Confidently use the runDatabaseAnalytics tool if the admin asks for shop performance, sales data, or general lists.
                5. TOOL ROUTING RULE: If the admin asks to search, filter, or list orders by anything other than a specific customer's PSID (for example: searching by Delivery Method, Payment Method, Date, or Price), you MUST route the request to the 'runDatabaseAnalytics' tool. The basic order tools cannot handle complex SQL filtering.
                6. CONTEXT ISOLATION: Safely drop previous context ONLY when a completely new topic is initiated. During an ongoing multi-turn task (like gathering missing parameters for an update), you MUST remember the intent and parameters from the immediate previous messages. Do not hallucinate IDs.
                7. ZERO-RESULT FALLBACK: If the 'runDatabaseAnalytics' SQL query returns empty data ([]), you must reply exclusively with 'No records found' and offer no further explanation. For all other CRM tools (like manageOrders or handleComplaints), you must naturally relay the tool's text response back to the admin.
                """;
        }

        try {
            System.out.println("Manager AI: Attempting conversation with Primary Engine (OpenRouter)...");
            return primaryClient.prompt()
                    .system(s -> s.text(systemInstruction)
                            .param("contextContactId", currentContactId != null ? currentContactId : "NONE")
                            .param("currentTenantId", safeTenantId)
                            .param("adminName", safeAdminName))
                    .user(moderatorPrompt)
                    .advisors(a -> {
                        // Apply the safe memoryId to prevent amnesia or cross-tenant hallucinations
                        a.param("chat_memory_conversation_id", memoryId);
                        a.param("chat_client_max_tool_calls", 3);
                    })
                    .call()
                    .content();

        } catch (Exception openRouterException) {

            System.out.println("Manager AI: OpenRouter failed (" + openRouterException.getMessage() + "). Pivoting to Fallback Engine...");

            try {
                return fallbackClient.prompt()
                        .system(s -> s.text(systemInstruction)
                                .param("contextContactId", currentContactId != null ? currentContactId : "NONE")
                                .param("currentTenantId", safeTenantId)
                                .param("adminName", safeAdminName))
                        .user(moderatorPrompt)
                        .advisors(a -> a.param("chat_memory_conversation_id", memoryId)) // Applied here as well
                        .call()
                        .content();

            } catch (Exception geminiException) {
                return "I'm sorry, I am currently offline and cannot execute tools or update the database right now. Please try again later.";
            }
        }
    }
}