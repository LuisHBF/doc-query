package br.com.docquery.query.query.usecase;

import java.util.UUID;

public interface DeleteConversationHistoryUseCase {

    void handle(UUID documentId, UUID userId);

}
