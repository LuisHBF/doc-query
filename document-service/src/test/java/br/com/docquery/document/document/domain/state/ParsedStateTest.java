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
    @DisplayName("returns PARSED status")
    void returnsParsedStatus() {
        assertThat(state.status()).isEqualTo(DocumentStatus.PARSED);
    }

    @Test
    @DisplayName("transitions to IndexingState when startIndexing is called")
    void transitionsToIndexingStateWhenStartIndexingIsCalled() {
        DocumentState next = state.startIndexing();
        assertThat(next).isInstanceOf(IndexingState.class);
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
    @DisplayName("throws IllegalStateException when finishIndexing is called")
    void throwsWhenFinishIndexingCalled() {
        assertThatThrownBy(() -> state.finishIndexing())
                .isInstanceOf(IllegalStateException.class);
    }

}
