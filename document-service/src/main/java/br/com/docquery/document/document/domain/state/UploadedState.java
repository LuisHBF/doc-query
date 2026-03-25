package br.com.docquery.document.document.domain.state;

import br.com.docquery.document.document.domain.DocumentStatus;
import lombok.Value;

@Value
public class UploadedState implements DocumentState {

    @Override
    public DocumentState startParsing() {
        return new ParsingState();
    }

    @Override
    public DocumentState finishParsing(Integer chunkCount) {
        throw new IllegalStateException("Cannot call finishParsing from UPLOADED state");
    }

    @Override
    public DocumentState startIndexing() {
        throw new IllegalStateException("Cannot call startIndexing from UPLOADED state");
    }

    @Override
    public DocumentState finishIndexing() {
        throw new IllegalStateException("Cannot call finishIndexing from UPLOADED state");
    }

    @Override
    public DocumentState fail() {
        return new FailedState();
    }

    @Override
    public DocumentStatus status() {
        return DocumentStatus.UPLOADED;
    }

}
