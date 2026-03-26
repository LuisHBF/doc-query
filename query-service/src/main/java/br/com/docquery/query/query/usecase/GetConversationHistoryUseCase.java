package br.com.docquery.query.query.usecase;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface GetConversationHistoryUseCase {

    List<Response> handle(UUID documentId, UUID userId);

    @Value
    @Builder
    class Response {

        String question;
        String answer;
        LocalDateTime askedAt;

    }

}
