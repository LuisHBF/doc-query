package br.com.docquery.query.query.application;

import br.com.docquery.query.query.domain.ConversationHistoryRepository;
import br.com.docquery.query.query.usecase.GetConversationHistoryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetConversationHistoryAppService implements GetConversationHistoryUseCase {

    private final ConversationHistoryRepository conversationHistoryRepository;

    @Override
    public List<Response> handle(UUID documentId, UUID userId) {
        return conversationHistoryRepository.findByDocumentIdAndUserId(documentId, userId)
                .stream()
                .map(h -> Response.builder()
                        .question(h.getQuestion())
                        .answer(h.getAnswer())
                        .askedAt(h.getAskedAt())
                        .build())
                .toList();
    }

}
