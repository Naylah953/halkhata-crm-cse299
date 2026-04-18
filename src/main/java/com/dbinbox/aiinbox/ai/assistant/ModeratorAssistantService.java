package com.dbinbox.aiinbox.ai.assistant;

import com.dbinbox.aiinbox.ai.tools.ModeratorTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ModeratorAssistantService
{
    private final ChatClient chatClient;

    //initialise the chatclient for moderator
    public ModeratorAssistantService(ChatClient.Builder builder, ModeratorTools moderatorTools) {
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
                .user(u -> u.text(moderatorPrompt)
                        // Pass the 'active' contact ID as context so the AI knows who we are talking about
                        .param("contextContactId", currentContactId))
                .call()
                .content();
    }
}