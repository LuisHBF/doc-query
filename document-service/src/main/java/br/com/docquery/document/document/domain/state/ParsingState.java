package br.com.docquery.document.document.domain.state;

import br.com.docquery.document.document.domain.DocumentStatus;
import lombok.Value;

@Value
public class ParsingState implements DocumentState {

    @Override
    public DocumentState startParsing() {
        throw new IllegalStateException("Cannot call startParsing from PARSING state");
    }

    @Override
    public DocumentState finishParsing(Integer chunkCount) {
        return new ParsedState();
    }

    @Override
    public DocumentState startIndexing() {
        throw new IllegalStateException("Cannot call startIndexing from PARSING state");
    }

    @Override
    public DocumentState finishIndexing() {
        throw new IllegalStateException("Cannot call finishIndexing from PARSING state");
    }

    @Override
    public DocumentState fail() {
        return new FailedState();
    }

    @Override
    public DocumentStatus status() {
        return DocumentStatus.PARSING;
    }

}
