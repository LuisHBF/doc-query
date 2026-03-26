package br.com.docquery.document.document.application;

import br.com.docquery.document.document.domain.Document;
import br.com.docquery.document.document.domain.DocumentRepository;
import br.com.docquery.document.document.usecase.DeleteDocumentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteDocumentAppService implements DeleteDocumentUseCase {

    private final DocumentRepository documentRepository;

    @Override
    public void handle(UUID documentId, UUID userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        if (!document.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Document not found: " + documentId);
        }

        documentRepository.delete(documentId);
    }

}
