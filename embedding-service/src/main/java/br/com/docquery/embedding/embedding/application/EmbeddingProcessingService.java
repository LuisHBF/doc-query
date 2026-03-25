package br.com.docquery.embedding.embedding.application;

import br.com.docquery.commons.messaging.DocumentParsedEvent;
import br.com.docquery.embedding.embedding.infrastructure.persistence.DocumentChunkEntity;
import br.com.docquery.embedding.embedding.infrastructure.persistence.DocumentChunkJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingProcessingService {

    private final DocumentChunkJpaRepository documentChunkJpaRepository;
    private final EmbeddingModel embeddingModel;

    public void process(DocumentParsedEvent event) {
        List<UUID> chunkIds = event.getChunkIds();

        log.info("Processing {} chunks for document {}", chunkIds.size(), event.getDocumentId());

        List<DocumentChunkEntity> chunks = documentChunkJpaRepository.findAllById(chunkIds);

        List<String> contents = chunks.stream()
                .map(DocumentChunkEntity::getContent)
                .toList();

        List<float[]> embeddings = embeddingModel.embed(contents);

        AtomicInteger index = new AtomicInteger(0);

        List<DocumentChunkEntity> updatedChunks = chunks.stream()
                .map(chunk -> chunk.applyEmbedding(embeddings.get(index.getAndIncrement())))
                .toList();

        documentChunkJpaRepository.saveAll(updatedChunks);

        log.info("Successfully embedded {} chunks for document {}", chunks.size(), event.getDocumentId());
    }

}