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
    @DisplayName("returns UPLOADED status")
    void returnsUploadedStatus() {
        assertThat(state.status()).isEqualTo(DocumentStatus.UPLOADED);
    }

    @Test
    @DisplayName("transitions to ParsingState when startParsing is called")
    void transitionsToParsingStateWhenStartParsingIsCalled() {
        DocumentState next = state.startParsing();
        assertThat(next).isInstanceOf(ParsingState.class);
    }

    @Test
    @DisplayName("transitions to FailedState when fail is called")
    void transitionsToFailedStateWhenFailIsCalled() {
        DocumentState next = state.fail();
        assertThat(next).isInstanceOf(FailedState.class);
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

    @Test
    @DisplayName("throws IllegalStateException when finishIndexing is called")
    void throwsWhenFinishIndexingCalled() {
        assertThatThrownBy(() -> state.finishIndexing())
                .isInstanceOf(IllegalStateException.class);
    }

}
