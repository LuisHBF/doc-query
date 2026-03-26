package br.com.docquery.document.document.api;

import br.com.docquery.commons.web.CurrentUserId;
import br.com.docquery.document.document.usecase.DeleteDocumentUseCase;
import br.com.docquery.document.document.usecase.GetDocumentUseCase;
import br.com.docquery.document.document.usecase.ListDocumentsUseCase;
import br.com.docquery.document.document.usecase.UploadDocumentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final UploadDocumentUseCase uploadDocumentUseCase;
    private final GetDocumentUseCase getDocumentUseCase;
    private final ListDocumentsUseCase listDocumentsUseCase;
    private final DeleteDocumentUseCase deleteDocumentUseCase;

    @PostMapping
    public ResponseEntity<UUID> upload(@RequestParam("file") MultipartFile file,
                                       @CurrentUserId UUID userId) throws IOException {

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

    @GetMapping
    public ResponseEntity<List<ListDocumentsUseCase.Response>> listDocuments(@CurrentUserId UUID userId) {
        List<ListDocumentsUseCase.Response> documents = listDocumentsUseCase.handle(userId);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GetDocumentUseCase.Response> getDocument(@PathVariable UUID id,
                                                                   @CurrentUserId UUID userId) {

        GetDocumentUseCase.Response response = getDocumentUseCase.handle(id, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID id,
                                               @CurrentUserId UUID userId) {
        deleteDocumentUseCase.handle(id, userId);
        return ResponseEntity.noContent().build();
    }

}
