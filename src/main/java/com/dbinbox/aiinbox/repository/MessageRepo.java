package com.dbinbox.aiinbox.repository;


import com.dbinbox.aiinbox.model.Conversation;
import com.dbinbox.aiinbox.model.Message;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepo extends JpaRepository<Message, Integer>
{
    // Spring generates: SELECT * FROM Message ORDER BY created_at DESC
    List<Message> findAllByOrderByCreatedAtDesc();
    List<Message> findByContactIdOrderByCreatedAtAsc(String contactId);
    boolean existsByMetaMid(String metaMid);

    List<Message> findTop20ByConversationOrderByCreatedAtAsc(Conversation conversation);

    List<Message> findTop4ByConversationOrderByCreatedAtDesc(Conversation conversation);
}
