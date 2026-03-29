package br.com.docquery.query.query.api;

import br.com.docquery.query.query.infrastructure.web.WebConfig;
import br.com.docquery.query.query.usecase.DeleteConversationHistoryUseCase;
import br.com.docquery.query.query.usecase.GetConversationHistoryUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HistoryController.class)
@Import(WebConfig.class)
class HistoryControllerTest {

    private static final String USER_ID_HEADER = "X-Api-Gateway-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetConversationHistoryUseCase getConversationHistoryUseCase;

    @MockBean
    private DeleteConversationHistoryUseCase deleteConversationHistoryUseCase;

    @Test
    @DisplayName("GET /documents/{id}/history returns 200 OK with conversation entries in response order")
    void getHistoryReturns200WithConversationEntries() throws Exception {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        GetConversationHistoryUseCase.Response entry = GetConversationHistoryUseCase.Response.builder()
                .question("What is the conclusion?")
                .answer("The conclusion shows positive results.")
                .askedAt(LocalDateTime.of(2025, 1, 1, 10, 0))
                .build();
        when(getConversationHistoryUseCase.handle(documentId, userId)).thenReturn(List.of(entry));

        mockMvc.perform(get("/documents/{id}/history", documentId)
                        .header(USER_ID_HEADER, userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].question").value("What is the conclusion?"))
                .andExpect(jsonPath("$[0].answer").value("The conclusion shows positive results."));
    }

    @Test
    @DisplayName("DELETE /documents/{id}/history returns 204 NO CONTENT and delegates to DeleteConversationHistoryUseCase")
    void deleteHistoryReturns204AndDelegatesToUseCase() throws Exception {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        mockMvc.perform(delete("/documents/{id}/history", documentId)
                        .header(USER_ID_HEADER, userId.toString()))
                .andExpect(status().isNoContent());

        verify(deleteConversationHistoryUseCase).handle(eq(documentId), eq(userId));
    }
}
