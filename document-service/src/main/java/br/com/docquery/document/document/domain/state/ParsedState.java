package br.com.docquery.document.document.domain.state;

import br.com.docquery.document.document.domain.DocumentStatus;
import lombok.Value;

@Value
public class ParsedState implements DocumentState {

    @Override
    public DocumentState startParsing() {
        throw new IllegalStateException("Cannot call startParsing from PARSED state");
    }

    @Override
    public DocumentState finishParsing(Integer chunkCount) {
        throw new IllegalStateException("Cannot call finishParsing from PARSED state");
    }

    @Override
    public DocumentState startIndexing() {
        return new IndexingState();
    }

    @Override
    public DocumentState finishIndexing() {
        throw new IllegalStateException("Cannot call finishIndexing from PARSED state");
    }

    @Override
    public DocumentState fail() {
        return new FailedState();
    }

    @Override
    public DocumentStatus status() {
        return DocumentStatus.PARSED;
    }

}
