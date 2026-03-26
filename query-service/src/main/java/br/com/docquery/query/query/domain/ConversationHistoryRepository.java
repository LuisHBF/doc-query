package br.com.docquery.query.query.domain;

import java.util.List;
import java.util.UUID;

public interface ConversationHistoryRepository {

    void save(ConversationHistory history);

    List<ConversationHistory> findByDocumentIdAndUserId(UUID documentId, UUID userId);

    void deleteByDocumentIdAndUserId(UUID documentId, UUID userId);

}