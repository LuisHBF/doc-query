package br.com.docquery.query.query.infrastructure.persistence;

import br.com.docquery.query.query.domain.ConversationHistory;
import br.com.docquery.query.query.domain.ConversationHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ConversationHistoryRepositoryImpl implements ConversationHistoryRepository {

    private final ConversationHistoryJpaRepository conversationHistoryJpaRepository;

    @Override
    public void save(ConversationHistory history) {
        ConversationHistoryEntity entity = ConversationHistoryEntity.builder()
                .id(history.getId())
                .documentId(history.getDocumentId())
                .userId(history.getUserId())
                .question(history.getQuestion())
                .answer(history.getAnswer())
                .askedAt(history.getAskedAt())
                .build();

        conversationHistoryJpaRepository.save(entity);
    }

    @Override
    public List<ConversationHistory> findByDocumentIdAndUserId(UUID documentId, UUID userId) {
        return conversationHistoryJpaRepository
                .findByDocumentIdAndUserIdOrderByAskedAtAsc(documentId, userId)
                .stream()
                .map(entity -> ConversationHistory.builder()
                        .id(entity.getId())
                        .documentId(entity.getDocumentId())
                        .userId(entity.getUserId())
                        .question(entity.getQuestion())
                        .answer(entity.getAnswer())
                        .askedAt(entity.getAskedAt())
                        .build())
                .toList();
    }

    @Override
    public void deleteByDocumentIdAndUserId(UUID documentId, UUID userId) {
        conversationHistoryJpaRepository.deleteByDocumentIdAndUserId(documentId, userId);
    }

}