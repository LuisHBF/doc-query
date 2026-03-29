package br.com.docquery.query.query.application;

import br.com.docquery.query.query.domain.ConversationHistoryRepository;
import br.com.docquery.query.query.infrastructure.ai.RagService;
import br.com.docquery.query.query.infrastructure.persistence.DocumentChunkRepository;
import br.com.docquery.query.query.usecase.ChatWithDocumentUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatWithDocumentUseCaseTest {

    @Mock
    private RagService ragService;

    @Mock
    private ConversationHistoryRepository conversationHistoryRepository;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @InjectMocks
    private ChatWithDocumentAppService chatWithDocumentAppService;

    @Test
    @DisplayName("returns a non-null SseEmitter when document exists and belongs to the requesting user")
    void returnsNonNullSseEmitterWhenDocumentExistsAndBelongsToUser() {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ChatWithDocumentUseCase.Command command = ChatWithDocumentUseCase.Command.builder()
                .documentId(documentId)
                .userId(userId)
                .question("What is this document about?")
                .build();
        when(documentChunkRepository.existsByDocumentIdAndUserId(documentId, userId)).thenReturn(true);
        when(conversationHistoryRepository.findByDocumentIdAndUserId(documentId, userId))
                .thenReturn(List.of());

        SseEmitter emitter = chatWithDocumentAppService.handle(command);

        assertThat(emitter).isNotNull();
    }

    @Test
    @DisplayName("delegates to RagService with the correct document ID, question, history and user ID")
    void delegatesToRagServiceWithCorrectArguments() {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String question = "Summarise the conclusions";
        ChatWithDocumentUseCase.Command command = ChatWithDocumentUseCase.Command.builder()
                .documentId(documentId)
                .userId(userId)
                .question(question)
                .build();
        when(documentChunkRepository.existsByDocumentIdAndUserId(documentId, userId)).thenReturn(true);
        when(conversationHistoryRepository.findByDocumentIdAndUserId(documentId, userId))
                .thenReturn(List.of());

        chatWithDocumentAppService.handle(command);

        verify(ragService).streamAnswer(
                eq(documentId),
                eq(question),
                anyList(),
                any(SseEmitter.class),
                eq(userId)
        );
    }

    @Test
    @DisplayName("throws ResponseStatusException with 404 when no indexed chunks exist for the document and user")
    void throwsNotFoundWhenNoIndexedChunksExistForDocumentAndUser() {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ChatWithDocumentUseCase.Command command = ChatWithDocumentUseCase.Command.builder()
                .documentId(documentId)
                .userId(userId)
                .question("What is this about?")
                .build();
        when(documentChunkRepository.existsByDocumentIdAndUserId(documentId, userId)).thenReturn(false);

        assertThatThrownBy(() -> chatWithDocumentAppService.handle(command))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }
}
