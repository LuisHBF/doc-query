package br.com.docquery.document.document.domain;

import br.com.docquery.document.document.domain.state.DocumentState;
import br.com.docquery.document.document.domain.state.FailedState;
import br.com.docquery.document.document.domain.state.IndexedState;
import br.com.docquery.document.document.domain.state.IndexingState;
import br.com.docquery.document.document.domain.state.ParsedState;
import br.com.docquery.document.document.domain.state.ParsingState;
import br.com.docquery.document.document.domain.state.UploadedState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentStateMachineTest {

    private Document buildDocument(DocumentState state) {
        return Document.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .fileName("test.pdf")
                .fileSizeBytes(1024L)
                .mimeType("application/pdf")
                .state(state)
                .chunkCount(0)
                .createdAt(LocalDateTime.now())
                .indexedAt(null)
                .build();
    }

    @Test
    @DisplayName("UPLOADED → startParsing() → status becomes PARSING")
    void uploadedStartParsingTransitionsToParsing() {
        Document document = buildDocument(new UploadedState());

        Document result = document.startParsing();

        assertThat(result.getStatus()).isEqualTo(DocumentStatus.PARSING);
    }

    @Test
    @DisplayName("PARSING → finishParsing(42) → status becomes PARSED and chunkCount is set")
    void parsingFinishParsingTransitionsToParsedWithChunkCount() {
        Document document = buildDocument(new ParsingState());

        Document result = document.finishParsing(42);

        assertThat(result.getStatus()).isEqualTo(DocumentStatus.PARSED);
        assertThat(result.getChunkCount()).isEqualTo(42);
    }

    @Test
    @DisplayName("PARSED → startIndexing() → status becomes INDEXING")
    void parsedStartIndexingTransitionsToIndexing() {
        Document document = buildDocument(new ParsedState());

        Document result = document.startIndexing();

        assertThat(result.getStatus()).isEqualTo(DocumentStatus.INDEXING);
    }

    @Test
    @DisplayName("INDEXING → finishIndexing() → status becomes INDEXED and indexedAt is set")
    void indexingFinishIndexingTransitionsToIndexedWithTimestamp() {
        Document document = buildDocument(new IndexingState());

        Document result = document.finishIndexing();

        assertThat(result.getStatus()).isEqualTo(DocumentStatus.INDEXED);
        assertThat(result.getIndexedAt()).isNotNull();
    }

    @Test
    @DisplayName("UPLOADED → fail() → status becomes FAILED")
    void uploadedFailTransitionsToFailed() {
        Document document = buildDocument(new UploadedState());

        Document result = document.fail();

        assertThat(result.getStatus()).isEqualTo(DocumentStatus.FAILED);
    }

    @Test
    @DisplayName("PARSING → fail() → status becomes FAILED")
    void parsingFailTransitionsToFailed() {
        Document document = buildDocument(new ParsingState());

        Document result = document.fail();

        assertThat(result.getStatus()).isEqualTo(DocumentStatus.FAILED);
    }

    @Test
    @DisplayName("PARSED → fail() → status becomes FAILED")
    void parsedFailTransitionsToFailed() {
        Document document = buildDocument(new ParsedState());

        Document result = document.fail();

        assertThat(result.getStatus()).isEqualTo(DocumentStatus.FAILED);
    }

    @Test
    @DisplayName("INDEXING → fail() → status becomes FAILED")
    void indexingFailTransitionsToFailed() {
        Document document = buildDocument(new IndexingState());

        Document result = document.fail();

        assertThat(result.getStatus()).isEqualTo(DocumentStatus.FAILED);
    }

    @Test
    @DisplayName("UPLOADED → finishParsing() → throws IllegalStateException")
    void uploadedFinishParsingThrows() {
        Document document = buildDocument(new UploadedState());

        assertThatThrownBy(() -> document.finishParsing(1))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("UPLOADED → startIndexing() → throws IllegalStateException")
    void uploadedStartIndexingThrows() {
        Document document = buildDocument(new UploadedState());

        assertThatThrownBy(document::startIndexing)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("PARSING → startParsing() → throws IllegalStateException")
    void parsingStartParsingThrows() {
        Document document = buildDocument(new ParsingState());

        assertThatThrownBy(document::startParsing)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("PARSED → finishParsing() → throws IllegalStateException")
    void parsedFinishParsingThrows() {
        Document document = buildDocument(new ParsedState());

        assertThatThrownBy(() -> document.finishParsing(1))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("INDEXING → startIndexing() → throws IllegalStateException")
    void indexingStartIndexingThrows() {
        Document document = buildDocument(new IndexingState());

        assertThatThrownBy(document::startIndexing)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("INDEXED → fail() → throws IllegalStateException because a fully indexed document cannot be reverted")
    void indexedFailThrows() {
        Document document = buildDocument(new IndexedState());

        assertThatThrownBy(document::fail)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("FAILED → startParsing() → throws IllegalStateException")
    void failedStartParsingThrows() {
        Document document = buildDocument(new FailedState());

        assertThatThrownBy(document::startParsing)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("DocumentState.from maps each DocumentStatus to the correct state implementation")
    void documentStateFromMapsAllStatusesCorrectly() {
        assertThat(DocumentState.from(DocumentStatus.UPLOADED)).isInstanceOf(UploadedState.class);
        assertThat(DocumentState.from(DocumentStatus.PARSING)).isInstanceOf(ParsingState.class);
        assertThat(DocumentState.from(DocumentStatus.PARSED)).isInstanceOf(ParsedState.class);
        assertThat(DocumentState.from(DocumentStatus.INDEXING)).isInstanceOf(IndexingState.class);
        assertThat(DocumentState.from(DocumentStatus.INDEXED)).isInstanceOf(IndexedState.class);
        assertThat(DocumentState.from(DocumentStatus.FAILED)).isInstanceOf(FailedState.class);
    }
}
