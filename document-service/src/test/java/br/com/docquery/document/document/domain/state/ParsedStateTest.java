package br.com.docquery.document.document.domain.state;

import br.com.docquery.document.document.domain.DocumentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ParsedState")
class ParsedStateTest {

    private ParsedState state;

    @BeforeEach
    void setUp() {
        state = new ParsedState();
    }

    @Test
    @DisplayName("should return PARSED status")
    void shouldReturnParsedStatus() {
        assertThat(state.status()).isEqualTo(DocumentStatus.PARSED);
    }

    @Test
    @DisplayName("should transition to IndexingState when startIndexing is called")
    void shouldTransitionToIndexingState() {
        DocumentState next = state.startIndexing();
        assertThat(next).isInstanceOf(IndexingState.class);
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
    @DisplayName("should throw IllegalStateException when finishIndexing is called")
    void shouldThrowWhenFinishIndexingCalled() {
        assertThatThrownBy(() -> state.finishIndexing())
                .isInstanceOf(IllegalStateException.class);
    }

}
