package br.com.docquery.document.document.domain;

import br.com.docquery.document.document.domain.state.DocumentState;
import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@With
@Builder
public class Document {

    UUID id;
    UUID userId;
    String fileName;
    Long fileSizeBytes;
    String mimeType;
    DocumentState state;
    Integer chunkCount;
    LocalDateTime createdAt;
    LocalDateTime indexedAt;

    public DocumentStatus getStatus() {
        return state.status();
    }

    public Document startParsing() {
        return this.withState(state.startParsing());
    }

    public Document finishParsing(Integer chunkCount) {
        return this.withState(state.finishParsing(chunkCount)).withChunkCount(chunkCount);
    }

    public Document startIndexing() {
        return this.withState(state.startIndexing());
    }

    public Document finishIndexing() {
        return this.withState(state.finishIndexing());
    }

    public Document fail() {
        return this.withState(state.fail());
    }

}
