package br.com.docquery.document.document.usecase;

import br.com.docquery.document.document.domain.DocumentStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ListDocumentsUseCase {

    List<Response> handle(UUID userId);

    @Value
    @Builder
    class Response {

        UUID id;
        String fileName;
        Long fileSizeBytes;
        DocumentStatus status;
        LocalDateTime createdAt;
        LocalDateTime indexedAt;

    }

}
