package com.yourcompany.assistant.model;

import com.yourcompany.assistant.enums.ConversationState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "conversations")
public class Conversation {

    @Id
    private String phoneNumber;

    private ConversationState currentState;

    private String month;
    private String year;
    private Integer currentCommessaIndex;

    @Builder.Default
    private List<String> commesseAttiveIds = new ArrayList<>();  // IDs delle commesse attive da processare

    @Builder.Default
    private List<Commessa> commesse = new ArrayList<>();  // Commesse compilate con giornate

    private LocalDateTime lastUpdated;
    private LocalDateTime createdAt;

    public void addCommessa(Commessa commessa) {
        if (this.commesse == null) {
            this.commesse = new ArrayList<>();
        }
        this.commesse.add(commessa);
    }

    public boolean hasMoreCommesseToProcess() {
        if (commesseAttiveIds == null || commesse == null) {
            return false;
        }
        return commesse.size() < commesseAttiveIds.size();
    }

    public void reset() {
        this.currentState = ConversationState.INITIAL;
        this.month = null;
        this.year = null;
        this.currentCommessaIndex = 0;
        this.commesseAttiveIds = new ArrayList<>();
        this.commesse = new ArrayList<>();
    }
}
