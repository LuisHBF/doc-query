package br.com.docquery.document.document.domain.state;

import br.com.docquery.document.document.domain.DocumentStatus;
import lombok.Value;

@Value
public class IndexingState implements DocumentState {

    @Override
    public DocumentState startParsing() {
        throw new IllegalStateException("Cannot call startParsing from INDEXING state");
    }

    @Override
    public DocumentState finishParsing(Integer chunkCount) {
        throw new IllegalStateException("Cannot call finishParsing from INDEXING state");
    }

    @Override
    public DocumentState startIndexing() {
        throw new IllegalStateException("Cannot call startIndexing from INDEXING state");
    }

    @Override
    public DocumentState finishIndexing() {
        return new IndexedState();
    }

    @Override
    public DocumentState fail() {
        return new FailedState();
    }

    @Override
    public DocumentStatus status() {
        return DocumentStatus.INDEXING;
    }

}
