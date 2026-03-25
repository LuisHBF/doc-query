package br.com.docquery.document.document.domain.state;

import br.com.docquery.document.document.domain.DocumentStatus;

public interface DocumentState {

    DocumentState startParsing();
    DocumentState finishParsing(Integer chunkCount);
    DocumentState startIndexing();
    DocumentState finishIndexing();
    DocumentState fail();
    DocumentStatus status();

    static DocumentState from(DocumentStatus status) {
        return switch (status) {
            case UPLOADED  -> new UploadedState();
            case PARSING   -> new ParsingState();
            case PARSED    -> new ParsedState();
            case INDEXING  -> new IndexingState();
            case INDEXED   -> new IndexedState();
            case FAILED    -> new FailedState();
        };
    }

}