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
    @DisplayName("should delegate getStatus to current state")
    void shouldDelegateGetStatusToCurrentState() {
        assertThat(uploaded.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
    }

    @Test
    @DisplayName("should transition through the full happy path")
    void shouldTransitionThroughFullHappyPath() {
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
    @DisplayName("should not mutate the original instance after startParsing")
    void shouldNotMutateOriginalAfterStartParsing() {
        Document parsing = uploaded.startParsing();

        assertThat(uploaded.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
        assertThat(parsing.getStatus()).isEqualTo(DocumentStatus.PARSING);
        assertThat(uploaded).isNotSameAs(parsing);
    }

    @Test
    @DisplayName("should update chunkCount immutably when finishParsing is called")
    void shouldUpdateChunkCountImmutably() {
        Document parsing = uploaded.startParsing();
        Document parsed = parsing.finishParsing(30);

        assertThat(parsed.getChunkCount()).isEqualTo(30);
        assertThat(parsing.getChunkCount()).isNull();
    }

    @Test
    @DisplayName("should transition to FAILED from UPLOADED")
    void shouldTransitionToFailedFromUploaded() {
        Document failed = uploaded.fail();

        assertThat(failed.getStatus()).isEqualTo(DocumentStatus.FAILED);
        assertThat(failed.getState()).isInstanceOf(FailedState.class);
    }

    @Test
    @DisplayName("should transition to FAILED from PARSING")
    void shouldTransitionToFailedFromParsing() {
        Document failed = uploaded.startParsing().fail();

        assertThat(failed.getStatus()).isEqualTo(DocumentStatus.FAILED);
        assertThat(failed.getState()).isInstanceOf(FailedState.class);
    }

    @Test
    @DisplayName("should transition to FAILED from PARSED")
    void shouldTransitionToFailedFromParsed() {
        Document failed = uploaded.startParsing().finishParsing(5).fail();

        assertThat(failed.getStatus()).isEqualTo(DocumentStatus.FAILED);
        assertThat(failed.getState()).isInstanceOf(FailedState.class);
    }

    @Test
    @DisplayName("should transition to FAILED from INDEXING")
    void shouldTransitionToFailedFromIndexing() {
        Document failed = uploaded.startParsing().finishParsing(5).startIndexing().fail();

        assertThat(failed.getStatus()).isEqualTo(DocumentStatus.FAILED);
        assertThat(failed.getState()).isInstanceOf(FailedState.class);
    }

    @Test
    @DisplayName("should throw IllegalStateException when finishParsing is called from UPLOADED")
    void shouldThrowWhenFinishParsingCalledFromUploaded() {
        assertThatThrownBy(() -> uploaded.finishParsing(10))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("should throw IllegalStateException when startIndexing is called from UPLOADED")
    void shouldThrowWhenStartIndexingCalledFromUploaded() {
        assertThatThrownBy(() -> uploaded.startIndexing())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("should throw IllegalStateException when finishIndexing is called from PARSED")
    void shouldThrowWhenFinishIndexingCalledFromParsed() {
        Document parsed = uploaded.startParsing().finishParsing(5);

        assertThatThrownBy(() -> parsed.finishIndexing())
                .isInstanceOf(IllegalStateException.class);
    }

}
