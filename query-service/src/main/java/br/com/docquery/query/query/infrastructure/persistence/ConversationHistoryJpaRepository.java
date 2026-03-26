package br.com.docquery.query.query.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConversationHistoryJpaRepository extends JpaRepository<ConversationHistoryEntity, UUID> {

    List<ConversationHistoryEntity> findByDocumentIdAndUserIdOrderByAskedAtAsc(UUID documentId, UUID userId);

    void deleteByDocumentIdAndUserId(UUID documentId, UUID userId);

}