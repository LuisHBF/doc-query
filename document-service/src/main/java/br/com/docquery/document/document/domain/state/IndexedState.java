package br.com.docquery.document.document.domain.state;

import br.com.docquery.document.document.domain.DocumentStatus;
import lombok.Value;

@Value
public class IndexedState implements DocumentState {

    @Override
    public DocumentState startParsing() {
        throw new IllegalStateException("Cannot call startParsing from INDEXED state");
    }

    @Override
    public DocumentState finishParsing(Integer chunkCount) {
        throw new IllegalStateException("Cannot call finishParsing from INDEXED state");
    }

    @Override
    public DocumentState startIndexing() {
        throw new IllegalStateException("Cannot call startIndexing from INDEXED state");
    }

    @Override
    public DocumentState finishIndexing() {
        throw new IllegalStateException("Cannot call finishIndexing from INDEXED state");
    }

    @Override
    public DocumentState fail() {
        throw new IllegalStateException("Cannot call fail from INDEXED state");
    }

    @Override
    public DocumentStatus status() {
        return DocumentStatus.INDEXED;
    }

}
