package br.com.docquery.document.document.domain.state;

import br.com.docquery.document.document.domain.DocumentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FailedState")
class FailedStateTest {

    private FailedState state;

    @BeforeEach
    void setUp() {
        state = new FailedState();
    }

    @Test
    @DisplayName("should return FAILED status")
    void shouldReturnFailedStatus() {
        assertThat(state.status()).isEqualTo(DocumentStatus.FAILED);
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

    @Test
    @DisplayName("should throw IllegalStateException when finishIndexing is called")
    void shouldThrowWhenFinishIndexingCalled() {
        assertThatThrownBy(() -> state.finishIndexing())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("should throw IllegalStateException when fail is called")
    void shouldThrowWhenFailCalled() {
        assertThatThrownBy(() -> state.fail())
                .isInstanceOf(IllegalStateException.class);
    }

}
