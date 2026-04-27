package com.dbinbox.aiinbox.repository;

import com.dbinbox.aiinbox.model.Contact;
import com.dbinbox.aiinbox.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationRepo extends JpaRepository<Conversation, Long> {


    Optional<Conversation> findByContactAndIsActiveTrue(Contact contact);
}