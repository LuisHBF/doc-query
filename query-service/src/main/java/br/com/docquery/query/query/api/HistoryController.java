package br.com.docquery.query.query.api;

import br.com.docquery.commons.web.CurrentUserId;
import br.com.docquery.query.query.usecase.DeleteConversationHistoryUseCase;
import br.com.docquery.query.query.usecase.GetConversationHistoryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class HistoryController {

    private final GetConversationHistoryUseCase getConversationHistoryUseCase;
    private final DeleteConversationHistoryUseCase deleteConversationHistoryUseCase;

    @GetMapping("/{id}/history")
    public ResponseEntity<List<GetConversationHistoryUseCase.Response>> getHistory(
            @PathVariable UUID id,
            @CurrentUserId UUID userId) {

        List<GetConversationHistoryUseCase.Response> history =
                getConversationHistoryUseCase.handle(id, userId);

        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/{id}/history")
    public ResponseEntity<Void> deleteHistory(
            @PathVariable UUID id,
            @CurrentUserId UUID userId) {

        deleteConversationHistoryUseCase.handle(id, userId);
        return ResponseEntity.noContent().build();
    }

}
