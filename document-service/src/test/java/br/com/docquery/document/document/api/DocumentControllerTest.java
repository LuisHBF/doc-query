package br.com.docquery.document.document.api;

import br.com.docquery.document.document.domain.DocumentStatus;
import br.com.docquery.document.document.infrastructure.web.WebConfig;
import br.com.docquery.document.document.usecase.DeleteDocumentUseCase;
import br.com.docquery.document.document.usecase.GetDocumentUseCase;
import br.com.docquery.document.document.usecase.ListDocumentsUseCase;
import br.com.docquery.document.document.usecase.UploadDocumentUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@Import(WebConfig.class)
class DocumentControllerTest {

    private static final String USER_ID_HEADER = "X-Api-Gateway-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UploadDocumentUseCase uploadDocumentUseCase;

    @MockBean
    private GetDocumentUseCase getDocumentUseCase;

    @MockBean
    private ListDocumentsUseCase listDocumentsUseCase;

    @MockBean
    private DeleteDocumentUseCase deleteDocumentUseCase;

    @Test
    @DisplayName("POST /documents returns 202 ACCEPTED with the document UUID when upload succeeds")
    void uploadReturns202WithDocumentUuid() throws Exception {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(uploadDocumentUseCase.handle(any())).thenReturn(documentId);
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "pdf content".getBytes());

        mockMvc.perform(multipart("/documents")
                        .file(file)
                        .header(USER_ID_HEADER, userId.toString()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$").value(documentId.toString()));
    }

    @Test
    @DisplayName("GET /documents returns 200 OK with a list of document summaries for the authenticated user")
    void listDocumentsReturns200WithDocumentSummaries() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        ListDocumentsUseCase.Response doc = ListDocumentsUseCase.Response.builder()
                .id(documentId)
                .fileName("report.pdf")
                .fileSizeBytes(2048L)
                .status(DocumentStatus.INDEXED)
                .createdAt(LocalDateTime.of(2025, 1, 1, 10, 0))
                .indexedAt(null)
                .build();
        when(listDocumentsUseCase.handle(userId)).thenReturn(List.of(doc));

        mockMvc.perform(get("/documents")
                        .header(USER_ID_HEADER, userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(documentId.toString()))
                .andExpect(jsonPath("$[0].fileName").value("report.pdf"))
                .andExpect(jsonPath("$[0].status").value("INDEXED"));
    }

    @Test
    @DisplayName("GET /documents/{id} returns 200 OK with full document details including chunk count and mime type")
    void getDocumentReturns200WithDocumentDetails() throws Exception {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        GetDocumentUseCase.Response response = GetDocumentUseCase.Response.builder()
                .id(documentId)
                .fileName("report.pdf")
                .fileSizeBytes(4096L)
                .mimeType("application/pdf")
                .status(DocumentStatus.INDEXED)
                .chunkCount(10)
                .createdAt(LocalDateTime.of(2025, 1, 1, 10, 0))
                .indexedAt(null)
                .build();
        when(getDocumentUseCase.handle(documentId, userId)).thenReturn(response);

        mockMvc.perform(get("/documents/{id}", documentId)
                        .header(USER_ID_HEADER, userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(documentId.toString()))
                .andExpect(jsonPath("$.fileName").value("report.pdf"))
                .andExpect(jsonPath("$.chunkCount").value(10))
                .andExpect(jsonPath("$.mimeType").value("application/pdf"));
    }

    @Test
    @DisplayName("DELETE /documents/{id} returns 204 NO CONTENT and delegates to DeleteDocumentUseCase with document ID and user ID")
    void deleteDocumentReturns204AndDelegatesToUseCase() throws Exception {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        mockMvc.perform(delete("/documents/{id}", documentId)
                        .header(USER_ID_HEADER, userId.toString()))
                .andExpect(status().isNoContent());

        verify(deleteDocumentUseCase).handle(eq(documentId), eq(userId));
    }
}
