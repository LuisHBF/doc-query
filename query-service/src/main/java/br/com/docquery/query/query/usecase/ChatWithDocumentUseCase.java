package br.com.docquery.query.query.usecase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

public interface ChatWithDocumentUseCase {

    SseEmitter handle(Command command);

    @Value
    @Builder
    class Command {

        @NotNull(message = "Document ID is required")
        UUID documentId;

        @NotNull(message = "User ID is required")
        UUID userId;

        @NotBlank(message = "Question is required")
        String question;

    }

}