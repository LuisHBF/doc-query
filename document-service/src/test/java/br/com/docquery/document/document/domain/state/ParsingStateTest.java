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
    @DisplayName("should return PARSING status")
    void shouldReturnParsingStatus() {
        assertThat(state.status()).isEqualTo(DocumentStatus.PARSING);
    }

    @Test
    @DisplayName("should transition to ParsedState when finishParsing is called")
    void shouldTransitionToParsedState() {
        DocumentState next = state.finishParsing(42);
        assertThat(next).isInstanceOf(ParsedState.class);
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
    @DisplayName("should throw IllegalStateException when startIndexing is called")
    void shouldThrowWhenStartIndexingCalled() {
        assertThatThrownBy(() -> state.startIndexing())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("should throw IllegalStateException when finishIndexing is called")
    void shouldThrowWhenFinishIndexingCalled() {
        assertThatThrownBy(() -> state.finishIndexing())
                .isInstanceOf(IllegalStateException.class);
    }

}
