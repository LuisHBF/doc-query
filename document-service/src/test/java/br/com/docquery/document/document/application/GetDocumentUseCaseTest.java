package br.com.docquery.document.document.application;

import br.com.docquery.document.document.domain.Document;
import br.com.docquery.document.document.domain.DocumentRepository;
import br.com.docquery.document.document.domain.DocumentStatus;
import br.com.docquery.document.document.domain.state.IndexedState;
import br.com.docquery.document.document.usecase.GetDocumentUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetDocumentUseCaseTest {

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private GetDocumentAppService getDocumentAppService;

    @Test
    @DisplayName("returns document response when document exists and belongs to the requesting user")
    void returnsDocumentResponseWhenDocumentExistsAndBelongsToUser() {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime indexedAt = LocalDateTime.now();
        Document document = Document.builder()
                .id(documentId)
                .userId(userId)
                .fileName("report.pdf")
                .fileSizeBytes(4096L)
                .mimeType("application/pdf")
                .state(new IndexedState())
                .chunkCount(12)
                .createdAt(createdAt)
                .indexedAt(indexedAt)
                .build();
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));

        GetDocumentUseCase.Response response = getDocumentAppService.handle(documentId, userId);

        assertThat(response.getId()).isEqualTo(documentId);
        assertThat(response.getFileName()).isEqualTo("report.pdf");
        assertThat(response.getFileSizeBytes()).isEqualTo(4096L);
        assertThat(response.getMimeType()).isEqualTo("application/pdf");
        assertThat(response.getStatus()).isEqualTo(DocumentStatus.INDEXED);
        assertThat(response.getChunkCount()).isEqualTo(12);
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        assertThat(response.getIndexedAt()).isEqualTo(indexedAt);
    }

    @Test
    @DisplayName("throws IllegalArgumentException when document does not exist")
    void throwsWhenDocumentDoesNotExist() {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getDocumentAppService.handle(documentId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Document not found");
    }

    @Test
    @DisplayName("throws IllegalArgumentException when document exists but belongs to a different user — same message to prevent enumeration")
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

        assertThatThrownBy(() -> getDocumentAppService.handle(documentId, requestingUserId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Document not found");
    }
}
