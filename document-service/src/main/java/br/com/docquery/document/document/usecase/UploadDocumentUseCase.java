package br.com.docquery.document.document.usecase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

public interface UploadDocumentUseCase {

    UUID handle(Command command);

    @Value
    @Builder
    class Command {

        @NotNull(message = "User ID is required")
        UUID userId;

        @NotBlank(message = "File name is required")
        String fileName;

        @NotNull(message = "File size is required")
        @Positive(message = "File size must be positive")
        Long fileSizeBytes;

        @NotBlank(message = "Mime type is required")
        String mimeType;

        @NotNull(message = "File content is required")
        byte[] content;

    }

}