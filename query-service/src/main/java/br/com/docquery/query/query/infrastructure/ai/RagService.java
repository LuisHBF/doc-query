package br.com.docquery.query.query.infrastructure.ai;

import br.com.docquery.query.query.domain.ConversationHistory;
import br.com.docquery.query.query.domain.ConversationHistoryRepository;
import br.com.docquery.query.query.infrastructure.persistence.DocumentChunkEntity;
import br.com.docquery.query.query.infrastructure.persistence.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Orchestrates the full Retrieval-Augmented Generation (RAG) pipeline.
 *
 * <p>Pipeline steps:
 * <ol>
 *   <li>Embed the user's question into a vector representation.</li>
 *   <li>Retrieve the most similar document chunks from the vector store.</li>
 *   <li>Assemble the retrieved chunks into a plain-text context block.</li>
 *   <li>Build the message list, interleaving conversation history with the current question and context.</li>
 *   <li>Stream the LLM response token by token via Server-Sent Events (SSE).</li>
 *   <li>Persist the completed exchange to the conversation history.</li>
 * </ol>
 *
 * <p>Streaming is executed on a virtual thread so the calling HTTP thread is released immediately.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private static final int TOP_K = 5;
    private static final String SYSTEM_PROMPT = """
            You are a helpful assistant that answers questions based exclusively on the provided document context.
            If the answer is not found in the context, say "I could not find this information in the document."
            Do not use any knowledge outside the provided context.
            Always respond in the same language the user asked the question in.
            """;

    private final EmbeddingModel embeddingModel;
    private final ChatClient chatClient;
    private final DocumentChunkRepository documentChunkRepository;
    private final ConversationHistoryRepository conversationHistoryRepository;

    /**
     * Executes the RAG pipeline and streams the answer to the given {@link SseEmitter}.
     *
     * <p>The method returns immediately; the actual work runs on a virtual thread
     * so the calling HTTP thread is not blocked during LLM inference.
     *
     * @param documentId the document to scope the retrieval to
     * @param question   the user's question in natural language
     * @param history    previous exchanges in the current conversation session
     * @param emitter    the SSE emitter through which tokens are pushed to the client
     * @param userId     the authenticated user, used when persisting the history entry
     */
    public void streamAnswer(UUID documentId,
                             String question,
                             List<ConversationHistory> history,
                             SseEmitter emitter,
                             UUID userId) {

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.execute(() -> {
                try {
                    float[] queryEmbedding = embedQuestion(question);
                    List<DocumentChunkEntity> chunks = retrieveSimilarChunks(documentId, queryEmbedding);
                    String context = assembleContext(chunks);
                    List<Message> messages = buildMessages(history, context, question);

                    streamLlmResponse(messages, emitter, documentId, userId, question);

                } catch (Exception e) {
                    log.error("Error in RAG pipeline", e);
                    emitter.completeWithError(e);
                }
            });
        }
    }

    /**
     * Converts the user's question into a float-vector embedding
     * so it can be compared against stored chunk embeddings.
     *
     * @param question the raw question text
     * @return the embedding vector
     */
    private float[] embedQuestion(String question) {
        return embeddingModel.embed(question);
    }

    /**
     * Queries the vector store for the {@code TOP_K} chunks whose embeddings
     * are closest to the query embedding, scoped to a single document.
     *
     * @param documentId     the document to search within
     * @param queryEmbedding the embedding of the user's question
     * @return the most semantically similar chunks
     */
    private List<DocumentChunkEntity> retrieveSimilarChunks(UUID documentId, float[] queryEmbedding) {
        return documentChunkRepository.findSimilarChunks(documentId, queryEmbedding, TOP_K);
    }

    /**
     * Concatenates the text content of the retrieved chunks into a single
     * context block separated by blank lines, ready to be injected into the prompt.
     *
     * @param chunks the chunks retrieved from the vector store
     * @return the assembled plain-text context
     */
    private String assembleContext(List<DocumentChunkEntity> chunks) {
        StringBuilder context = new StringBuilder();
        for (DocumentChunkEntity chunk : chunks) {
            context.append(chunk.getContent()).append("\n\n");
        }
        return context.toString();
    }

    /**
     * Builds the ordered message list for the LLM, preserving conversation continuity.
     *
     * <p>Structure: [history turn 1 user, history turn 1 assistant, …, current user message with context].
     * The system prompt and retrieved context are injected into the final user message.
     *
     * @param history  previous conversation exchanges
     * @param context  the assembled document context
     * @param question the current user question
     * @return the message list ready for the chat model
     */
    private List<Message> buildMessages(List<ConversationHistory> history,
                                        String context,
                                        String question) {
        List<Message> messages = new ArrayList<>();

        String userPromptWithContext = SYSTEM_PROMPT +
                "\n\nDocument context:\n" + context +
                "\n\nQuestion: " + question;

        for (ConversationHistory entry : history) {
            messages.add(new UserMessage(entry.getQuestion()));
            messages.add(new AssistantMessage(entry.getAnswer()));
        }

        messages.add(new UserMessage(userPromptWithContext));
        return messages;
    }

    /**
     * Initiates the reactive SSE stream against the LLM.
     *
     * <p>Each token is forwarded to the client as it arrives. On completion,
     * the full answer is persisted and the stream is closed. On error, the emitter
     * is completed with the throwable so the client receives an error event.
     *
     * @param messages   the full message list including history and context
     * @param emitter    the SSE emitter to push tokens to
     * @param documentId used when persisting the history entry
     * @param userId     used when persisting the history entry
     * @param question   used when persisting the history entry
     */
    private void streamLlmResponse(List<Message> messages,
                                   SseEmitter emitter,
                                   UUID documentId,
                                   UUID userId,
                                   String question) {
        StringBuilder fullAnswer = new StringBuilder();

        chatClient.prompt()
                .messages(messages)
                .stream()
                .chatResponse()
                .doOnNext(response -> {
                    String token = response.getResult().getOutput().getText();
                    if (token != null && !token.isEmpty()) {
                        fullAnswer.append(token);
                        sendToken(emitter, token);
                    }
                })
                .doOnComplete(() -> {
                    saveHistory(documentId, userId, question, fullAnswer.toString());
                    completeStream(emitter);
                })
                .doOnError(error -> {
                    log.error("Error during RAG streaming", error);
                    emitter.completeWithError(error);
                })
                .subscribe();
    }

    /**
     * Sends a single token to the client through the SSE emitter.
     *
     * @param emitter the SSE emitter
     * @param token   the text token to send
     */
    private void sendToken(SseEmitter emitter, String token) {
        try {
            emitter.send(SseEmitter.event().data(token));
        } catch (IOException e) {
            log.error("Error sending SSE token", e);
        }
    }

    /**
     * Sends the terminal {@code [DONE]} SSE event and closes the emitter,
     * signalling the client that the stream has ended.
     *
     * @param emitter the SSE emitter to close
     */
    private void completeStream(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().name("done").data("[DONE]"));
            emitter.complete();
        } catch (IOException e) {
            log.error("Error completing SSE", e);
        }
    }

    /**
     * Persists the completed question-answer exchange to the conversation history,
     * enabling future turns to include prior context.
     *
     * @param documentId the document the conversation is about
     * @param userId     the user who asked the question
     * @param question   the original question
     * @param answer     the full answer accumulated from the streamed tokens
     */
    private void saveHistory(UUID documentId, UUID userId, String question, String answer) {
        ConversationHistory history = ConversationHistory.builder()
                .id(UUID.randomUUID())
                .documentId(documentId)
                .userId(userId)
                .question(question)
                .answer(answer)
                .askedAt(LocalDateTime.now())
                .build();

        conversationHistoryRepository.save(history);
    }

}
