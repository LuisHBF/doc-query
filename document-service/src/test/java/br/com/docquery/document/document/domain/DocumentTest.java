package br.com.docquery.document.document.domain;

import br.com.docquery.document.document.domain.state.FailedState;
import br.com.docquery.document.document.domain.state.IndexedState;
import br.com.docquery.document.document.domain.state.IndexingState;
import br.com.docquery.document.document.domain.state.ParsedState;
import br.com.docquery.document.document.domain.state.ParsingState;
import br.com.docquery.document.document.domain.state.UploadedState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Document state delegation")
class DocumentTest {

    private Document uploaded;

    @BeforeEach
    void setUp() {
        uploaded = Document.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .fileName("report.pdf")
                .fileSizeBytes(2048L)
                .mimeType("application/pdf")
                .state(new UploadedState())
                .chunkCount(null)
                .createdAt(LocalDateTime.now())
                .indexedAt(null)
                .build();
    }

    @Test
    @DisplayName("delegates getStatus to the current state")
    void delegatesGetStatusToCurrentState() {
        assertThat(uploaded.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
    }

    @Test
    @DisplayName("transitions through the full happy path UPLOADED → PARSING → PARSED → INDEXING → INDEXED")
    void transitionsThroughFullHappyPath() {
        Document parsing = uploaded.startParsing();
        assertThat(parsing.getStatus()).isEqualTo(DocumentStatus.PARSING);
        assertThat(parsing.getState()).isInstanceOf(ParsingState.class);

        Document parsed = parsing.finishParsing(15);
        assertThat(parsed.getStatus()).isEqualTo(DocumentStatus.PARSED);
        assertThat(parsed.getState()).isInstanceOf(ParsedState.class);
        assertThat(parsed.getChunkCount()).isEqualTo(15);

        Document indexing = parsed.startIndexing();
        assertThat(indexing.getStatus()).isEqualTo(DocumentStatus.INDEXING);
        assertThat(indexing.getState()).isInstanceOf(IndexingState.class);

        Document indexed = indexing.finishIndexing();
        assertThat(indexed.getStatus()).isEqualTo(DocumentStatus.INDEXED);
        assertThat(indexed.getState()).isInstanceOf(IndexedState.class);
    }

    @Test
    @DisplayName("does not mutate the original instance after startParsing — Document is immutable")
    void doesNotMutateOriginalInstanceAfterStartParsing() {
        Document parsing = uploaded.startParsing();

        assertThat(uploaded.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
        assertThat(parsing.getStatus()).isEqualTo(DocumentStatus.PARSING);
        assertThat(uploaded).isNotSameAs(parsing);
    }

    @Test
    @DisplayName("updates chunkCount immutably when finishParsing is called — original PARSING instance retains null chunkCount")
    void updatesChunkCountImmutablyWhenFinishParsingIsCalled() {
        Document parsing = uploaded.startParsing();
        Document parsed = parsing.finishParsing(30);

        assertThat(parsed.getChunkCount()).isEqualTo(30);
        assertThat(parsing.getChunkCount()).isNull();
    }

    @Test
    @DisplayName("transitions to FAILED from UPLOADED when fail is called")
    void transitionsToFailedFromUploaded() {
        Document failed = uploaded.fail();

        assertThat(failed.getStatus()).isEqualTo(DocumentStatus.FAILED);
        assertThat(failed.getState()).isInstanceOf(FailedState.class);
    }

    @Test
    @DisplayName("transitions to FAILED from PARSING when fail is called")
    void transitionsToFailedFromParsing() {
        Document failed = uploaded.startParsing().fail();

        assertThat(failed.getStatus()).isEqualTo(DocumentStatus.FAILED);
        assertThat(failed.getState()).isInstanceOf(FailedState.class);
    }

    @Test
    @DisplayName("transitions to FAILED from PARSED when fail is called")
    void transitionsToFailedFromParsed() {
        Document failed = uploaded.startParsing().finishParsing(5).fail();

        assertThat(failed.getStatus()).isEqualTo(DocumentStatus.FAILED);
        assertThat(failed.getState()).isInstanceOf(FailedState.class);
    }

    @Test
    @DisplayName("transitions to FAILED from INDEXING when fail is called")
    void transitionsToFailedFromIndexing() {
        Document failed = uploaded.startParsing().finishParsing(5).startIndexing().fail();

        assertThat(failed.getStatus()).isEqualTo(DocumentStatus.FAILED);
        assertThat(failed.getState()).isInstanceOf(FailedState.class);
    }

    @Test
    @DisplayName("throws IllegalStateException when finishParsing is called from UPLOADED")
    void throwsWhenFinishParsingCalledFromUploaded() {
        assertThatThrownBy(() -> uploaded.finishParsing(10))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("throws IllegalStateException when startIndexing is called from UPLOADED")
    void throwsWhenStartIndexingCalledFromUploaded() {
        assertThatThrownBy(() -> uploaded.startIndexing())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("throws IllegalStateException when finishIndexing is called from PARSED")
    void throwsWhenFinishIndexingCalledFromParsed() {
        Document parsed = uploaded.startParsing().finishParsing(5);

        assertThatThrownBy(() -> parsed.finishIndexing())
                .isInstanceOf(IllegalStateException.class);
    }

}
