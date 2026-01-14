package com.yourcompany.assistant.repository;

import com.yourcompany.assistant.model.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ConversationRepository extends MongoRepository<Conversation, String> {
    
    /**
     * Trova tutte le conversazioni aggiornate prima di una certa data
     * (utile per pulizia sessioni scadute)
     */
    List<Conversation> findByLastUpdatedBefore(LocalDateTime dateTime);
}
