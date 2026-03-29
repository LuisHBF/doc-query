package br.com.docquery.query.query.application;

import br.com.docquery.query.query.domain.ConversationHistory;
import br.com.docquery.query.query.domain.ConversationHistoryRepository;
import br.com.docquery.query.query.usecase.DeleteConversationHistoryUseCase;
import br.com.docquery.query.query.usecase.GetConversationHistoryUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationHistoryUseCaseTest {

    @Mock
    private ConversationHistoryRepository conversationHistoryRepository;

    @InjectMocks
    private GetConversationHistoryAppService getConversationHistoryAppService;

    @InjectMocks
    private DeleteConversationHistoryAppService deleteConversationHistoryAppService;

    @Test
    @DisplayName("returns empty list when no conversation history exists for the given document and user")
    void returnsEmptyListWhenNoHistoryExists() {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(conversationHistoryRepository.findByDocumentIdAndUserId(documentId, userId))
                .thenReturn(List.of());

        List<GetConversationHistoryUseCase.Response> result =
                getConversationHistoryAppService.handle(documentId, userId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("maps ConversationHistory domain objects to response DTOs preserving question, answer and timestamp")
    void mapsDomainObjectsToResponseDtosCorrectly() {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime askedAt = LocalDateTime.of(2025, 6, 15, 10, 30);
        ConversationHistory history = ConversationHistory.builder()
                .id(UUID.randomUUID())
                .documentId(documentId)
                .userId(userId)
                .question("What is the conclusion?")
                .answer("The conclusion is positive.")
                .askedAt(askedAt)
                .build();
        when(conversationHistoryRepository.findByDocumentIdAndUserId(documentId, userId))
                .thenReturn(List.of(history));

        List<GetConversationHistoryUseCase.Response> result =
                getConversationHistoryAppService.handle(documentId, userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getQuestion()).isEqualTo("What is the conclusion?");
        assertThat(result.get(0).getAnswer()).isEqualTo("The conclusion is positive.");
        assertThat(result.get(0).getAskedAt()).isEqualTo(askedAt);
    }

    @Test
    @DisplayName("preserves the order of history entries as returned by the repository")
    void preservesOrderOfHistoryEntriesFromRepository() {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ConversationHistory first = ConversationHistory.builder()
                .id(UUID.randomUUID())
                .documentId(documentId)
                .userId(userId)
                .question("First question")
                .answer("First answer")
                .askedAt(LocalDateTime.now().minusHours(2))
                .build();
        ConversationHistory second = ConversationHistory.builder()
                .id(UUID.randomUUID())
                .documentId(documentId)
                .userId(userId)
                .question("Second question")
                .answer("Second answer")
                .askedAt(LocalDateTime.now().minusHours(1))
                .build();
        when(conversationHistoryRepository.findByDocumentIdAndUserId(documentId, userId))
                .thenReturn(List.of(first, second));

        List<GetConversationHistoryUseCase.Response> result =
                getConversationHistoryAppService.handle(documentId, userId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getQuestion()).isEqualTo("First question");
        assertThat(result.get(1).getQuestion()).isEqualTo("Second question");
    }

    @Test
    @DisplayName("delegates delete to repository with the correct document ID and user ID")
    void deleteDelegatestoRepositoryWithCorrectIds() {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        assertThatCode(() -> deleteConversationHistoryAppService.handle(documentId, userId))
                .doesNotThrowAnyException();

        verify(conversationHistoryRepository).deleteByDocumentIdAndUserId(documentId, userId);
    }
}
