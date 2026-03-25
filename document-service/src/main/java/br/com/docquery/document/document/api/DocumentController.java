package br.com.docquery.document.document.api;

import br.com.docquery.document.document.usecase.UploadDocumentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final UploadDocumentUseCase uploadDocumentUseCase;

    @PostMapping
    public ResponseEntity<UUID> upload(@RequestParam("file") MultipartFile file,
                                       @RequestHeader("X-Api-Gateway-User-Id") UUID userId) throws IOException {

        UploadDocumentUseCase.Command command = UploadDocumentUseCase.Command.builder()
                .userId(userId)
                .fileName(file.getOriginalFilename())
                .fileSizeBytes(file.getSize())
                .mimeType(file.getContentType())
                .content(file.getBytes())
                .build();

        UUID documentId = uploadDocumentUseCase.handle(command);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(documentId);
    }

}