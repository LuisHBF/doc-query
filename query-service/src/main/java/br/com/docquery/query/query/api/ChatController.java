package br.com.docquery.query.query.api;

import br.com.docquery.commons.web.CurrentUserId;
import br.com.docquery.query.query.usecase.ChatWithDocumentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class ChatController {

    private final ChatWithDocumentUseCase chatWithDocumentUseCase;

    @PostMapping(value = "/{id}/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@PathVariable UUID id,
                           @RequestBody ChatRequest request,
                           @CurrentUserId UUID userId) {

        ChatWithDocumentUseCase.Command command = ChatWithDocumentUseCase.Command.builder()
                .documentId(id)
                .userId(userId)
                .question(request.question())
                .build();

        return chatWithDocumentUseCase.handle(command);
    }
}