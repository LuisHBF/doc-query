package br.com.docquery.document.document.application;

import br.com.docquery.commons.messaging.DocumentParsedEvent;
import br.com.docquery.document.BaseIntegrationTest;
import br.com.docquery.document.document.domain.Document;
import br.com.docquery.document.document.domain.DocumentRepository;
import br.com.docquery.document.document.domain.DocumentStatus;
import br.com.docquery.document.document.domain.state.DocumentState;
import br.com.docquery.document.document.infrastructure.messaging.publisher.DocumentEventPublisher;
import br.com.docquery.document.document.infrastructure.parsing.DocumentChunker;
import br.com.docquery.document.document.infrastructure.parsing.DocumentParser;
import br.com.docquery.document.document.infrastructure.persistence.DocumentChunkJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentProcessingServiceTest extends BaseIntegrationTest {

    @MockBean
    private DocumentRepository documentRepository;

    @MockBean
    private DocumentChunkJpaRepository documentChunkJpaRepository;

    @MockBean
    private DocumentParser documentParser;

    @MockBean
    private DocumentChunker documentChunker;

    @MockBean
    private DocumentEventPublisher documentEventPublisher;

    @Autowired
    private DocumentProcessingService documentProcessingService;

    @Test
    @DisplayName("transitions document to PARSING then PARSED, saves all chunks, and publishes DocumentParsedEvent when parser succeeds")
    void transitionsDocumentStateSavesChunksAndPublishesEventOnSuccess() {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Document document = Document.builder()
                .id(documentId)
                .userId(userId)
                .fileName("test.pdf")
                .fileSizeBytes(1024L)
                .mimeType("application/pdf")
                .state(DocumentState.from(DocumentStatus.UPLOADED))
                .createdAt(LocalDateTime.now())
                .build();
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentParser.extractText(any())).thenReturn("word1 word2 word3");
        when(documentChunker.chunk("word1 word2 word3")).thenReturn(List.of("word1 word2 word3"));

        documentProcessingService.process(documentId, new byte[]{1, 2, 3});

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(documentEventPublisher).publishDocumentParsed(any(DocumentParsedEvent.class)));
        verify(documentChunkJpaRepository).saveAll(any());
    }

    @Test
    @DisplayName("saves document with FAILED status when parser throws and does not publish any event")
    void savesDocumentAsFailedWhenParserThrows() {
        UUID documentId = UUID.randomUUID();
        Document document = Document.builder()
                .id(documentId)
                .userId(UUID.randomUUID())
                .fileName("corrupt.pdf")
                .fileSizeBytes(512L)
                .mimeType("application/pdf")
                .state(DocumentState.from(DocumentStatus.UPLOADED))
                .createdAt(LocalDateTime.now())
                .build();
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentParser.extractText(any())).thenThrow(new RuntimeException("Corrupt file"));
        ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);

        documentProcessingService.process(documentId, new byte[]{1});

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(documentRepository, atLeast(2)).save(documentCaptor.capture());
                    List<Document> saved = documentCaptor.getAllValues();
                    assertThat(saved).anyMatch(d -> d.getStatus() == DocumentStatus.FAILED);
                });
    }
}
