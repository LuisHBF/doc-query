package br.com.docquery.document.document.domain.state;

import br.com.docquery.document.document.domain.DocumentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("IndexingState")
class IndexingStateTest {

    private IndexingState state;

    @BeforeEach
    void setUp() {
        state = new IndexingState();
    }

    @Test
    @DisplayName("returns INDEXING status")
    void returnsIndexingStatus() {
        assertThat(state.status()).isEqualTo(DocumentStatus.INDEXING);
    }

    @Test
    @DisplayName("transitions to IndexedState when finishIndexing is called")
    void transitionsToIndexedStateWhenFinishIndexingIsCalled() {
        DocumentState next = state.finishIndexing();
        assertThat(next).isInstanceOf(IndexedState.class);
    }

    @Test
    @DisplayName("transitions to FailedState when fail is called")
    void transitionsToFailedStateWhenFailIsCalled() {
        DocumentState next = state.fail();
        assertThat(next).isInstanceOf(FailedState.class);
    }

    @Test
    @DisplayName("throws IllegalStateException when startParsing is called")
    void throwsWhenStartParsingCalled() {
        assertThatThrownBy(() -> state.startParsing())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("throws IllegalStateException when finishParsing is called")
    void throwsWhenFinishParsingCalled() {
        assertThatThrownBy(() -> state.finishParsing(10))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("throws IllegalStateException when startIndexing is called")
    void throwsWhenStartIndexingCalled() {
        assertThatThrownBy(() -> state.startIndexing())
                .isInstanceOf(IllegalStateException.class);
    }

}
