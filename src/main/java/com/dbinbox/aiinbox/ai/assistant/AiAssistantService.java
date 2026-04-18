/*package com.dbinbox.aiinbox.ai.assistant;

import com.dbinbox.aiinbox.ai.tools.ModeratorTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

@Service
public class AiAssistantService
{

    private final ChatClient chatClient;
    private final ModeratorTools crmTools;

    public AiAssistantService(ChatClient.Builder builder, ChatMemory chatMemory, ModeratorTools crmTools)
    {
        this.crmTools = crmTools;

        // Use the static builder method for the Advisor
        this.chatClient = builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    public String getResponse(String senderId, String userContent) {
        try {
            String systemPrompt = """
            You are a helpful Social CRM Assistant for a Bangladeshi business.
            The current user's PSID is: %s.
            
            When the moderator asks to 'save', 'update', or 'delete' the contact, 
            ALWAYS use this PSID for the 'psid' parameter in your tools.
            You understand Banglish and English.
            """.formatted(senderId);

            return this.chatClient.prompt()
                    .system(systemPrompt)
                    .user(userContent)
                    .tools(crmTools)
                    // Using the literal string key directly
                    .advisors(a -> a.param("chat_memory_conversation_id", senderId))
                    .call()
                    .content();

        } catch (Exception e) {
            System.err.println("AI Processing Error: " + e.getMessage());
            return "System is busy. We will get back to you soon!";
        }
    }
}*/