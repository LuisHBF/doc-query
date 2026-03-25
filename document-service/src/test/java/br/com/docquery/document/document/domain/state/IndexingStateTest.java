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
    @DisplayName("should return INDEXING status")
    void shouldReturnIndexingStatus() {
        assertThat(state.status()).isEqualTo(DocumentStatus.INDEXING);
    }

    @Test
    @DisplayName("should transition to IndexedState when finishIndexing is called")
    void shouldTransitionToIndexedState() {
        DocumentState next = state.finishIndexing();
        assertThat(next).isInstanceOf(IndexedState.class);
    }

    @Test
    @DisplayName("should transition to FailedState when fail is called")
    void shouldTransitionToFailedState() {
        DocumentState next = state.fail();
        assertThat(next).isInstanceOf(FailedState.class);
    }

    @Test
    @DisplayName("should throw IllegalStateException when startParsing is called")
    void shouldThrowWhenStartParsingCalled() {
        assertThatThrownBy(() -> state.startParsing())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("should throw IllegalStateException when finishParsing is called")
    void shouldThrowWhenFinishParsingCalled() {
        assertThatThrownBy(() -> state.finishParsing(10))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("should throw IllegalStateException when startIndexing is called")
    void shouldThrowWhenStartIndexingCalled() {
        assertThatThrownBy(() -> state.startIndexing())
                .isInstanceOf(IllegalStateException.class);
    }

}
