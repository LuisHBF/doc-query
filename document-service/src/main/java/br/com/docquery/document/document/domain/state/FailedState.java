package br.com.docquery.document.document.domain.state;

import br.com.docquery.document.document.domain.DocumentStatus;
import lombok.Value;

@Value
public class FailedState implements DocumentState {

    @Override
    public DocumentState startParsing() {
        throw new IllegalStateException("Cannot call startParsing from FAILED state");
    }

    @Override
    public DocumentState finishParsing(Integer chunkCount) {
        throw new IllegalStateException("Cannot call finishParsing from FAILED state");
    }

    @Override
    public DocumentState startIndexing() {
        throw new IllegalStateException("Cannot call startIndexing from FAILED state");
    }

    @Override
    public DocumentState finishIndexing() {
        throw new IllegalStateException("Cannot call finishIndexing from FAILED state");
    }

    @Override
    public DocumentState fail() {
        throw new IllegalStateException("Cannot call fail from FAILED state");
    }

    @Override
    public DocumentStatus status() {
        return DocumentStatus.FAILED;
    }

}
