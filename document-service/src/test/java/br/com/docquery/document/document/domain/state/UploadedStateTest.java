package br.com.docquery.document.document.domain.state;

import br.com.docquery.document.document.domain.DocumentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UploadedState")
class UploadedStateTest {

    private UploadedState state;

    @BeforeEach
    void setUp() {
        state = new UploadedState();
    }

    @Test
    @DisplayName("should return UPLOADED status")
    void shouldReturnUploadedStatus() {
        assertThat(state.status()).isEqualTo(DocumentStatus.UPLOADED);
    }

    @Test
    @DisplayName("should transition to ParsingState when startParsing is called")
    void shouldTransitionToParsingState() {
        DocumentState next = state.startParsing();
        assertThat(next).isInstanceOf(ParsingState.class);
    }

    @Test
    @DisplayName("should transition to FailedState when fail is called")
    void shouldTransitionToFailedState() {
        DocumentState next = state.fail();
        assertThat(next).isInstanceOf(FailedState.class);
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

}
