package br.com.docquery.document.document.application;

import br.com.docquery.document.document.domain.Document;
import br.com.docquery.document.document.domain.DocumentRepository;
import br.com.docquery.document.document.domain.state.UploadedState;
import br.com.docquery.document.document.usecase.UploadDocumentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadDocumentAppService implements UploadDocumentUseCase {

    private final DocumentRepository documentRepository;
    private final DocumentProcessingService documentProcessingService;

    @Override
    public UUID handle(Command command) {
        Document document = Document.builder()
                .id(UUID.randomUUID())
                .userId(command.getUserId())
                .fileName(command.getFileName())
                .fileSizeBytes(command.getFileSizeBytes())
                .mimeType(command.getMimeType())
                .state(new UploadedState())
                .chunkCount(0)
                .createdAt(LocalDateTime.now())
                .indexedAt(null)
                .build();

        UUID documentId = documentRepository.save(document);

        documentProcessingService.process(documentId, command.getContent());

        return documentId;
    }

}