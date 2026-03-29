package br.com.docquery.embedding.application;

import br.com.docquery.commons.messaging.DocumentIndexedEvent;
import br.com.docquery.commons.messaging.DocumentIndexingStartedEvent;
import br.com.docquery.commons.messaging.DocumentParsedEvent;
import br.com.docquery.embedding.infrastructure.messaging.publisher.DocumentEventPublisher;
import br.com.docquery.embedding.infrastructure.persistence.DocumentChunkEntity;
import br.com.docquery.embedding.infrastructure.persistence.DocumentChunkJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessEmbeddingUseCaseTest {

    @Mock
    private DocumentChunkJpaRepository documentChunkJpaRepository;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private DocumentEventPublisher documentEventPublisher;

    @InjectMocks
    private EmbeddingProcessingService embeddingProcessingService;

    @Test
    @DisplayName("publishes DocumentIndexingStartedEvent before generating embeddings")
    void publishesIndexingStartedEventBeforeGeneratingEmbeddings() {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        DocumentChunkEntity chunk = buildChunk(chunkId, documentId, "some text content");
        DocumentParsedEvent event = DocumentParsedEvent.builder()
                .documentId(documentId)
                .userId(userId)
                .chunkIds(List.of(chunkId))
                .build();
        when(documentChunkJpaRepository.findAllById(List.of(chunkId))).thenReturn(List.of(chunk));
        when(embeddingModel.embed(anyList())).thenReturn(List.of(new float[]{0.1f, 0.2f, 0.3f}));
        when(documentChunkJpaRepository.saveAll(anyList())).thenReturn(List.of(chunk));
        ArgumentCaptor<DocumentIndexingStartedEvent> captor =
                ArgumentCaptor.forClass(DocumentIndexingStartedEvent.class);

        embeddingProcessingService.process(event);

        verify(documentEventPublisher).publishDocumentIndexingStarted(captor.capture());
        assertThat(captor.getValue().getDocumentId()).isEqualTo(documentId);
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("generates embeddings for all chunk contents and persists the updated chunks")
    void generatesEmbeddingsForAllChunksAndPersistsThem() {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID chunkId1 = UUID.randomUUID();
        UUID chunkId2 = UUID.randomUUID();
        DocumentChunkEntity chunk1 = buildChunk(chunkId1, documentId, "first chunk text");
        DocumentChunkEntity chunk2 = buildChunk(chunkId2, documentId, "second chunk text");
        DocumentParsedEvent event = DocumentParsedEvent.builder()
                .documentId(documentId)
                .userId(userId)
                .chunkIds(List.of(chunkId1, chunkId2))
                .build();
        when(documentChunkJpaRepository.findAllById(List.of(chunkId1, chunkId2)))
                .thenReturn(List.of(chunk1, chunk2));
        float[] embedding1 = {0.1f, 0.2f};
        float[] embedding2 = {0.3f, 0.4f};
        when(embeddingModel.embed(List.of("first chunk text", "second chunk text")))
                .thenReturn(List.of(embedding1, embedding2));
        when(documentChunkJpaRepository.saveAll(anyList())).thenReturn(List.of(chunk1, chunk2));

        embeddingProcessingService.process(event);

        verify(embeddingModel).embed(List.of("first chunk text", "second chunk text"));
        verify(documentChunkJpaRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("publishes DocumentIndexedEvent after all chunks are successfully embedded and persisted")
    void publishesDocumentIndexedEventAfterAllChunksEmbedded() {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        DocumentChunkEntity chunk = buildChunk(chunkId, documentId, "chunk text");
        DocumentParsedEvent event = DocumentParsedEvent.builder()
                .documentId(documentId)
                .userId(userId)
                .chunkIds(List.of(chunkId))
                .build();
        when(documentChunkJpaRepository.findAllById(List.of(chunkId))).thenReturn(List.of(chunk));
        when(embeddingModel.embed(anyList())).thenReturn(List.of(new float[]{0.1f, 0.2f}));
        when(documentChunkJpaRepository.saveAll(anyList())).thenReturn(List.of(chunk));
        ArgumentCaptor<DocumentIndexedEvent> captor =
                ArgumentCaptor.forClass(DocumentIndexedEvent.class);

        embeddingProcessingService.process(event);

        verify(documentEventPublisher).publishDocumentIndexed(captor.capture());
        assertThat(captor.getValue().getDocumentId()).isEqualTo(documentId);
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
    }

    private DocumentChunkEntity buildChunk(UUID id, UUID documentId, String content) {
        return DocumentChunkEntity.builder()
                .id(id)
                .documentId(documentId)
                .chunkIndex(0)
                .content(content)
                .build();
    }
}
