package br.com.docquery.query.query.infrastructure.ai;

import br.com.docquery.query.query.domain.ConversationHistory;
import br.com.docquery.query.query.domain.ConversationHistoryRepository;
import br.com.docquery.query.query.infrastructure.persistence.DocumentChunkEntity;
import br.com.docquery.query.query.infrastructure.persistence.DocumentChunkRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private ChatClient chatClient;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private ConversationHistoryRepository conversationHistoryRepository;

    @InjectMocks
    private RagService ragService;

    @Test
    @DisplayName("embeds the question using EmbeddingModel before querying the vector store")
    void embedsQuestionBeforeQueryingVectorStore() throws Exception {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        float[] embedding = {0.1f, 0.2f, 0.3f};
        String question = "What is the conclusion?";
        SseEmitter emitter = buildMockedEmitter();
        stubChatClientChain(buildSingleTokenResponse("answer"));
        when(embeddingModel.embed(question)).thenReturn(embedding);
        when(documentChunkRepository.findSimilarChunks(any(), any(), eq(5)))
                .thenReturn(List.of());

        ragService.streamAnswer(documentId, question, List.of(), emitter, userId);

        verify(embeddingModel).embed(question);
    }

    @Test
    @DisplayName("queries vector store with document ID and question embedding to retrieve relevant chunks")
    void queriesVectorStoreWithDocumentIdAndQuestionEmbedding() throws Exception {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        float[] embedding = {0.5f, 0.6f};
        SseEmitter emitter = buildMockedEmitter();
        stubChatClientChain(buildSingleTokenResponse("answer"));
        when(embeddingModel.embed(anyString())).thenReturn(embedding);
        when(documentChunkRepository.findSimilarChunks(any(), any(), eq(5)))
                .thenReturn(List.of());

        ragService.streamAnswer(documentId, "What is this?", List.of(), emitter, userId);

        verify(documentChunkRepository).findSimilarChunks(eq(documentId), eq(embedding), eq(5));
    }

    @Test
    @DisplayName("persists conversation history with question and accumulated streamed answer after pipeline completes")
    void persistsConversationHistoryWithQuestionAndAnswerAfterPipelineCompletes() throws Exception {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String question = "What are the findings?";
        DocumentChunkEntity chunk = DocumentChunkEntity.builder()
                .id(UUID.randomUUID())
                .documentId(documentId)
                .chunkIndex(0)
                .content("The findings show improvement.")
                .build();
        SseEmitter emitter = buildMockedEmitter();
        stubChatClientChain(buildSingleTokenResponse("Based on the document, findings are positive."));
        when(embeddingModel.embed(question)).thenReturn(new float[]{0.1f});
        when(documentChunkRepository.findSimilarChunks(any(), any(), eq(5)))
                .thenReturn(List.of(chunk));
        ArgumentCaptor<ConversationHistory> historyCaptor =
                ArgumentCaptor.forClass(ConversationHistory.class);

        ragService.streamAnswer(documentId, question, List.of(), emitter, userId);

        verify(conversationHistoryRepository).save(historyCaptor.capture());
        ConversationHistory saved = historyCaptor.getValue();
        assertThat(saved.getDocumentId()).isEqualTo(documentId);
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getQuestion()).isEqualTo(question);
        assertThat(saved.getAnswer()).isEqualTo("Based on the document, findings are positive.");
        assertThat(saved.getAskedAt()).isNotNull();
    }

    @Test
    @DisplayName("includes prior conversation history in the messages sent to the LLM for context continuity")
    void includesPriorHistoryInMessagesForContextContinuity() throws Exception {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ConversationHistory priorTurn = ConversationHistory.builder()
                .id(UUID.randomUUID())
                .documentId(documentId)
                .userId(userId)
                .question("Previous question")
                .answer("Previous answer")
                .askedAt(LocalDateTime.now().minusMinutes(5))
                .build();
        SseEmitter emitter = buildMockedEmitter();
        ChatResponse priorTurnResponse = buildSingleTokenResponse("new answer");
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.StreamResponseSpec streamSpec = mock(ChatClient.StreamResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.messages(anyList())).thenReturn(requestSpec);
        when(requestSpec.stream()).thenReturn(streamSpec);
        when(streamSpec.chatResponse()).thenReturn(Flux.just(priorTurnResponse));
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{0.1f});
        when(documentChunkRepository.findSimilarChunks(any(), any(), eq(5))).thenReturn(List.of());
        ArgumentCaptor<List> messagesCaptor = ArgumentCaptor.forClass(List.class);

        ragService.streamAnswer(documentId, "Follow-up question", List.of(priorTurn), emitter, userId);

        verify(requestSpec).messages(messagesCaptor.capture());
        List<?> messages = messagesCaptor.getValue();
        assertThat(messages.size()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("completes the SSE emitter with error when the LLM throws during streaming")
    void completesEmitterWithErrorWhenLlmThrows() throws Exception {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SseEmitter emitter = mock(SseEmitter.class);
        RuntimeException llmError = new RuntimeException("LLM unavailable");
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.StreamResponseSpec streamSpec = mock(ChatClient.StreamResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.messages(anyList())).thenReturn(requestSpec);
        when(requestSpec.stream()).thenReturn(streamSpec);
        when(streamSpec.chatResponse()).thenReturn(Flux.error(llmError));
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{0.1f});
        when(documentChunkRepository.findSimilarChunks(any(), any(), eq(5))).thenReturn(List.of());

        ragService.streamAnswer(documentId, "question", List.of(), emitter, userId);

        verify(emitter).completeWithError(llmError);
    }

    private SseEmitter buildMockedEmitter() throws Exception {
        SseEmitter emitter = mock(SseEmitter.class);
        doNothing().when(emitter).send(any(SseEmitter.SseEventBuilder.class));
        doNothing().when(emitter).complete();
        return emitter;
    }

    private void stubChatClientChain(ChatResponse chatResponse) {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.StreamResponseSpec streamSpec = mock(ChatClient.StreamResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.messages(anyList())).thenReturn(requestSpec);
        when(requestSpec.stream()).thenReturn(streamSpec);
        when(streamSpec.chatResponse()).thenReturn(Flux.just(chatResponse));
    }

    private ChatResponse buildSingleTokenResponse(String text) {
        AssistantMessage assistantMessage = mock(AssistantMessage.class);
        Generation generation = mock(Generation.class);
        ChatResponse response = mock(ChatResponse.class);
        when(assistantMessage.getText()).thenReturn(text);
        when(generation.getOutput()).thenReturn(assistantMessage);
        when(response.getResult()).thenReturn(generation);
        return response;
    }
}
