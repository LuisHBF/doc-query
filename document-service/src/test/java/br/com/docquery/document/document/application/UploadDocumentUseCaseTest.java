package br.com.docquery.document.document.application;

import br.com.docquery.document.document.domain.Document;
import br.com.docquery.document.document.domain.DocumentRepository;
import br.com.docquery.document.document.domain.DocumentStatus;
import br.com.docquery.document.document.usecase.UploadDocumentUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadDocumentUseCaseTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentProcessingService documentProcessingService;

    @InjectMocks
    private UploadDocumentAppService uploadDocumentAppService;

    @Test
    @DisplayName("returns a new UUID after persisting the document with UPLOADED status")
    void returnsNewUuidAfterPersistingDocumentWithUploadedStatus() {
        UUID expectedId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        byte[] content = "pdf content".getBytes();
        UploadDocumentUseCase.Command command = UploadDocumentUseCase.Command.builder()
                .userId(userId)
                .fileName("report.pdf")
                .fileSizeBytes(2048L)
                .mimeType("application/pdf")
                .content(content)
                .build();
        when(documentRepository.save(any(Document.class))).thenReturn(expectedId);

        UUID result = uploadDocumentAppService.handle(command);

        assertThat(result).isEqualTo(expectedId);
    }

    @Test
    @DisplayName("persists document with UPLOADED status and correct metadata before triggering async processing")
    void persistsDocumentWithUploadedStatusAndCorrectMetadata() {
        UUID userId = UUID.randomUUID();
        byte[] content = "pdf content".getBytes();
        UploadDocumentUseCase.Command command = UploadDocumentUseCase.Command.builder()
                .userId(userId)
                .fileName("report.pdf")
                .fileSizeBytes(2048L)
                .mimeType("application/pdf")
                .content(content)
                .build();
        when(documentRepository.save(any(Document.class))).thenReturn(UUID.randomUUID());
        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);

        uploadDocumentAppService.handle(command);

        verify(documentRepository).save(captor.capture());
        Document saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getFileName()).isEqualTo("report.pdf");
        assertThat(saved.getFileSizeBytes()).isEqualTo(2048L);
        assertThat(saved.getMimeType()).isEqualTo("application/pdf");
        assertThat(saved.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("triggers async processing with the saved document ID and raw content")
    void triggersAsyncProcessingWithSavedDocumentIdAndContent() {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        byte[] content = "pdf content".getBytes();
        UploadDocumentUseCase.Command command = UploadDocumentUseCase.Command.builder()
                .userId(userId)
                .fileName("report.pdf")
                .fileSizeBytes(2048L)
                .mimeType("application/pdf")
                .content(content)
                .build();
        when(documentRepository.save(any(Document.class))).thenReturn(documentId);

        uploadDocumentAppService.handle(command);

        verify(documentProcessingService).process(eq(documentId), eq(content));
    }
}
