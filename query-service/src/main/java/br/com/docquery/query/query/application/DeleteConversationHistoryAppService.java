package br.com.docquery.query.query.application;

import br.com.docquery.query.query.domain.ConversationHistoryRepository;
import br.com.docquery.query.query.usecase.DeleteConversationHistoryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteConversationHistoryAppService implements DeleteConversationHistoryUseCase {

    private final ConversationHistoryRepository conversationHistoryRepository;

    @Override
    public void handle(UUID documentId, UUID userId) {
        conversationHistoryRepository.deleteByDocumentIdAndUserId(documentId, userId);
    }

}
