package br.com.docquery.document.document.application;

import br.com.docquery.document.document.domain.Document;
import br.com.docquery.document.document.domain.DocumentRepository;
import br.com.docquery.document.document.usecase.GetDocumentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetDocumentAppService implements GetDocumentUseCase {

    private final DocumentRepository documentRepository;

    @Override
    public Response handle(UUID documentId, UUID userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        if (!document.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Document not found");
        }

        return Response.builder()
                .id(document.getId())
                .fileName(document.getFileName())
                .fileSizeBytes(document.getFileSizeBytes())
                .mimeType(document.getMimeType())
                .status(document.getStatus())
                .chunkCount(document.getChunkCount())
                .createdAt(document.getCreatedAt())
                .indexedAt(document.getIndexedAt())
                .build();
    }

}