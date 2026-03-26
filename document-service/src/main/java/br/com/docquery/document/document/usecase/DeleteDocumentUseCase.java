package br.com.docquery.document.document.usecase;

import java.util.UUID;

public interface DeleteDocumentUseCase {

    void handle(UUID documentId, UUID userId);

}
