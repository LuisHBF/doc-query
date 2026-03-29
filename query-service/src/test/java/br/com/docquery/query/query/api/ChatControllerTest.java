package br.com.docquery.query.query.api;

import br.com.docquery.query.query.infrastructure.web.WebConfig;
import br.com.docquery.query.query.usecase.ChatWithDocumentUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@Import(WebConfig.class)
class ChatControllerTest {

    private static final String USER_ID_HEADER = "X-Api-Gateway-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatWithDocumentUseCase chatWithDocumentUseCase;

    @Test
    @DisplayName("POST /documents/{id}/chat starts async SSE response when use case returns an emitter")
    void chatStartsAsyncSseResponseWhenUseCaseReturnsEmitter() throws Exception {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(chatWithDocumentUseCase.handle(any())).thenReturn(new SseEmitter());

        mockMvc.perform(post("/documents/{id}/chat", documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What is the summary?\"}")
                        .header(USER_ID_HEADER, userId.toString()))
                .andExpect(request().asyncStarted());
    }

    @Test
    @DisplayName("POST /documents/{id}/chat delegates to ChatWithDocumentUseCase with document ID, user ID and question from the request")
    void chatDelegatesToUseCaseWithDocumentIdUserIdAndQuestion() throws Exception {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(chatWithDocumentUseCase.handle(any())).thenReturn(new SseEmitter());
        ArgumentCaptor<ChatWithDocumentUseCase.Command> captor =
                ArgumentCaptor.forClass(ChatWithDocumentUseCase.Command.class);

        mockMvc.perform(post("/documents/{id}/chat", documentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"What is the summary?\"}")
                .header(USER_ID_HEADER, userId.toString()));

        verify(chatWithDocumentUseCase).handle(captor.capture());
        assertThat(captor.getValue().getDocumentId()).isEqualTo(documentId);
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getQuestion()).isEqualTo("What is the summary?");
    }
}
