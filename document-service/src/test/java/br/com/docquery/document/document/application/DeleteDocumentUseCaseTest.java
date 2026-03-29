package br.com.docquery.document.document.application;

import br.com.docquery.document.document.domain.Document;
import br.com.docquery.document.document.domain.DocumentRepository;
import br.com.docquery.document.document.domain.state.IndexedState;
import br.com.docquery.document.document.usecase.DeleteDocumentUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteDocumentUseCaseTest {

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private DeleteDocumentAppService deleteDocumentAppService;

    @Test
    @DisplayName("deletes document when it exists and belongs to the requesting user")
    void deletesDocumentWhenItExistsAndBelongsToUser() {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Document document = Document.builder()
                .id(documentId)
                .userId(userId)
                .fileName("report.pdf")
                .fileSizeBytes(1024L)
                .mimeType("application/pdf")
                .state(new IndexedState())
                .chunkCount(5)
                .createdAt(LocalDateTime.now())
                .indexedAt(null)
                .build();
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));

        assertThatCode(() -> deleteDocumentAppService.handle(documentId, userId))
                .doesNotThrowAnyException();

        verify(documentRepository).delete(documentId);
    }

    @Test
    @DisplayName("throws IllegalArgumentException when document does not exist")
    void throwsWhenDocumentDoesNotExist() {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deleteDocumentAppService.handle(documentId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(documentId.toString());
    }

    @Test
    @DisplayName("throws IllegalArgumentException when document belongs to a different user — same message to prevent enumeration")
    void throwsWhenDocumentBelongsToDifferentUser() {
        UUID documentId = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();
        UUID requestingUserId = UUID.randomUUID();
        Document document = Document.builder()
                .id(documentId)
                .userId(ownerUserId)
                .fileName("report.pdf")
                .fileSizeBytes(1024L)
                .mimeType("application/pdf")
                .state(new IndexedState())
                .chunkCount(5)
                .createdAt(LocalDateTime.now())
                .indexedAt(null)
                .build();
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> deleteDocumentAppService.handle(documentId, requestingUserId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(documentId.toString());
    }
}
