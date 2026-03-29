package br.com.docquery.document.document.domain.state;

import br.com.docquery.document.document.domain.DocumentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("IndexedState")
class IndexedStateTest {

    private IndexedState state;

    @BeforeEach
    void setUp() {
        state = new IndexedState();
    }

    @Test
    @DisplayName("returns INDEXED status")
    void returnsIndexedStatus() {
        assertThat(state.status()).isEqualTo(DocumentStatus.INDEXED);
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

    @Test
    @DisplayName("throws IllegalStateException when finishIndexing is called")
    void throwsWhenFinishIndexingCalled() {
        assertThatThrownBy(() -> state.finishIndexing())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("throws IllegalStateException when fail is called — a fully indexed document cannot be reverted")
    void throwsWhenFailCalled() {
        assertThatThrownBy(() -> state.fail())
                .isInstanceOf(IllegalStateException.class);
    }

}
