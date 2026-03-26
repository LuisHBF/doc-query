package br.com.docquery.query.query.application;

import br.com.docquery.query.query.domain.ConversationHistory;
import br.com.docquery.query.query.domain.ConversationHistoryRepository;
import br.com.docquery.query.query.infrastructure.ai.RagService;
import br.com.docquery.query.query.usecase.ChatWithDocumentUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatWithDocumentAppService implements ChatWithDocumentUseCase {

    private final RagService ragService;
    private final ConversationHistoryRepository conversationHistoryRepository;

    @Override
    public SseEmitter handle(Command command) {
        List<ConversationHistory> history = conversationHistoryRepository
                .findByDocumentIdAndUserId(command.getDocumentId(), command.getUserId());

        SseEmitter emitter = new SseEmitter(180_000L);

        ragService.streamAnswer(
                command.getDocumentId(),
                command.getQuestion(),
                history,
                emitter,
                command.getUserId()
        );

        return emitter;
    }

}