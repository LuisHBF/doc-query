package br.com.docquery.document.document.domain.state;

import br.com.docquery.document.document.domain.DocumentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ParsingState")
class ParsingStateTest {

    private ParsingState state;

    @BeforeEach
    void setUp() {
        state = new ParsingState();
    }

    @Test
    @DisplayName("returns PARSING status")
    void returnsParsingStatus() {
        assertThat(state.status()).isEqualTo(DocumentStatus.PARSING);
    }

    @Test
    @DisplayName("transitions to ParsedState when finishParsing is called")
    void transitionsToParsedStateWhenFinishParsingIsCalled() {
        DocumentState next = state.finishParsing(42);
        assertThat(next).isInstanceOf(ParsedState.class);
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
    @DisplayName("throws IllegalStateException when startIndexing is called")
    void throwsWhenStartIndexingCalled() {
        assertThatThrownBy(() -> state.startIndexing())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("throws IllegalStateException when finishIndexing is called")
    void throwsWhenFinishIndexingCalled() {
        assertThatThrownBy(() -> state.finishIndexing())
                .isInstanceOf(IllegalStateException.class);
    }

}
