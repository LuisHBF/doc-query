package br.com.docquery.document.document.usecase;

import br.com.docquery.document.document.domain.DocumentStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

public interface GetDocumentUseCase {

    Response handle(UUID documentId, UUID userId);

    @Value
    @Builder
    class Response {

        UUID id;
        String fileName;
        Long fileSizeBytes;
        String mimeType;
        DocumentStatus status;
        Integer chunkCount;
        LocalDateTime createdAt;
        LocalDateTime indexedAt;

    }

}