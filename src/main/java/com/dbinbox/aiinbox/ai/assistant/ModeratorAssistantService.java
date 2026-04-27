package com.dbinbox.aiinbox.ai.assistant;

import com.dbinbox.aiinbox.ai.tools.ModeratorTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ModeratorAssistantService
{
    private final ChatClient chatClient;

    //initialise the chatclient for moderator
    public ModeratorAssistantService(ChatClient.Builder builder, ModeratorTools moderatorTools)
    {
        this.chatClient = builder
                // Use .tools() to register your @Tool-annotated methods
                .defaultTools(moderatorTools)
                .defaultSystem("You are a CRM Admin Assistant for the moderator...")
                .build();
    }

    //method to call the LLM
    public String useAssistant(String moderatorPrompt, String currentContactId)
    {
        return chatClient.prompt()
                .system(s -> s.text("""
                You are a CRM Admin Assistant. 
                CONTEXT: You are currently acting on the profile of Contact ID: {contextContactId}.
                
                RULES:
                1. When calling any tool (createContact, updateContact, deleteContact), 
                   you MUST use "{contextContactId}" for the 'psid' parameter.
                2. Do not ask the moderator for the PSID; it is provided in this context.
                3. If the moderator says "Update the name to X", use the 'updateContact' tool 
                   with psid="{contextContactId}" and name="X".
                """)
                        .param("contextContactId", currentContactId))
                .user(moderatorPrompt)
                .advisors(a -> a.param("chat_memory_conversation_id", currentContactId))
                .call()
                .content();
    }
}