package br.com.docquery.document.document.application;

import br.com.docquery.commons.messaging.DocumentParsedEvent;
import br.com.docquery.document.document.domain.Document;
import br.com.docquery.document.document.domain.DocumentRepository;
import br.com.docquery.document.document.infrastructure.messaging.publisher.DocumentEventPublisher;
import br.com.docquery.document.document.infrastructure.parsing.DocumentChunker;
import br.com.docquery.document.document.infrastructure.parsing.DocumentParser;
import br.com.docquery.document.document.infrastructure.persistence.DocumentChunkEntity;
import br.com.docquery.document.document.infrastructure.persistence.DocumentChunkJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkJpaRepository documentChunkJpaRepository;
    private final DocumentParser documentParser;
    private final DocumentChunker documentChunker;
    private final DocumentEventPublisher documentEventPublisher;

    @Async
    public void process(UUID documentId, byte[] content) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        try {
            Document parsing = document.startParsing();
            documentRepository.save(parsing);

            String text = documentParser.extractText(content);
            List<String> chunkTexts = documentChunker.chunk(text);

            AtomicInteger index = new AtomicInteger(0);

            List<DocumentChunkEntity> chunks = chunkTexts.stream()
                    .map(chunkText -> DocumentChunkEntity.builder()
                            .id(UUID.randomUUID())
                            .documentId(documentId)
                            .chunkIndex(index.getAndIncrement())
                            .content(chunkText)
                            .tokenCount(chunkText.split("\\s+").length)
                            .embeddedAt(null)
                            .build())
                    .toList();

            documentChunkJpaRepository.saveAll(chunks);

            Document parsed = parsing.finishParsing(chunkTexts.size());
            documentRepository.save(parsed);

            List<UUID> chunkIds = chunks.stream()
                    .map(DocumentChunkEntity::getId)
                    .toList();

            DocumentParsedEvent event = DocumentParsedEvent.builder()
                    .documentId(documentId)
                    .userId(document.getUserId())
                    .chunkIds(chunkIds)
                    .build();

            documentEventPublisher.publishDocumentParsed(event);

            log.info("Document {} parsed successfully — {} chunks created", documentId, chunkTexts.size());

        } catch (Exception e) {
            log.error("Failed to process document {}", documentId, e);
            Document failed = document.fail();
            documentRepository.save(failed);
        }
    }

}