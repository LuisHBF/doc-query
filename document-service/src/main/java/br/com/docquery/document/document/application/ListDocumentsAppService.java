package br.com.docquery.document.document.application;

import br.com.docquery.document.document.domain.Document;
import br.com.docquery.document.document.domain.DocumentRepository;
import br.com.docquery.document.document.usecase.ListDocumentsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListDocumentsAppService implements ListDocumentsUseCase {

    private final DocumentRepository documentRepository;

    @Override
    public List<Response> handle(UUID userId) {
        return documentRepository.findAllByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Response toResponse(Document document) {
        return Response.builder()
                .id(document.getId())
                .fileName(document.getFileName())
                .fileSizeBytes(document.getFileSizeBytes())
                .status(document.getStatus())
                .createdAt(document.getCreatedAt())
                .indexedAt(document.getIndexedAt())
                .build();
    }

}
