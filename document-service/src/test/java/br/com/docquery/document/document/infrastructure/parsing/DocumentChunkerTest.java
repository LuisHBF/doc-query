package br.com.docquery.document.document.infrastructure.parsing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentChunkerTest {

    private DocumentChunker chunker;

    @BeforeEach
    void setUp() {
        chunker = new DocumentChunker();
    }

    @Test
    @DisplayName("empty string produces one chunk with empty content because Java split(\"\\\\s+\") on \"\" returns [\"\"]")
    void emptyStringProducesOneChunkWithEmptyContent() {
        List<String> result = chunker.chunk("");

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEmpty();
    }

    @Test
    @DisplayName("single word returns one chunk containing that exact word")
    void singleWordReturnsSingleChunkWithThatWord() {
        List<String> result = chunker.chunk("hello");

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo("hello");
    }

    @Test
    @DisplayName("text with fewer than 450 words returns a single chunk containing all words")
    void textWithFewerThan450WordsReturnsSingleChunk() {
        String text = buildText(100);

        List<String> result = chunker.chunk(text);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).split("\\s+")).hasSize(100);
    }

    @Test
    @DisplayName("text with exactly 450 words returns a single chunk — 450 is the stride boundary (CHUNK_SIZE 500 minus OVERLAP 50)")
    void textWithExactly450WordsReturnsSingleChunk() {
        String text = buildText(450);

        List<String> result = chunker.chunk(text);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).split("\\s+")).hasSize(450);
    }

    @Test
    @DisplayName("text with 451 words crosses the stride boundary and returns two chunks — first has 451 words because min(CHUNK_SIZE, total) caps the window")
    void textWith451WordsReturnsTwoChunks() {
        String text = buildText(451);

        List<String> result = chunker.chunk(text);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).split("\\s+")).hasSize(451);
    }

    @Test
    @DisplayName("text with 501 words returns two chunks where the second begins with the 50-word overlap from the first")
    void textWith501WordsReturnsTwoChunksWithOverlap() {
        String text = buildText(501);

        List<String> result = chunker.chunk(text);

        assertThat(result).hasSize(2);
        String[] firstChunkWords = result.get(0).split("\\s+");
        String[] secondChunkWords = result.get(1).split("\\s+");
        assertThat(firstChunkWords).hasSize(500);
        assertThat(secondChunkWords[0]).isEqualTo(firstChunkWords[450]);
    }

    @Test
    @DisplayName("text with exactly 900 words returns two chunks — chunk 1 has 500 words, chunk 2 has 450 words (words 450-899)")
    void textWithExactly900WordsReturnsTwoChunks() {
        String text = buildText(900);

        List<String> result = chunker.chunk(text);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).split("\\s+")).hasSize(500);
        assertThat(result.get(1).split("\\s+")).hasSize(450);
    }

    @Test
    @DisplayName("text with 901 words crosses the second stride boundary and returns three chunks")
    void textWith901WordsReturnsThreeChunks() {
        String text = buildText(901);

        List<String> result = chunker.chunk(text);

        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("consecutive chunks share exactly 50 words at their boundary preserving context across chunk edges")
    void consecutiveChunksShareExactly50WordsAtBoundary() {
        String text = buildText(600);

        List<String> result = chunker.chunk(text);

        assertThat(result).hasSize(2);
        String[] first = result.get(0).split("\\s+");
        String[] second = result.get(1).split("\\s+");
        for (int i = 0; i < 50; i++) {
            assertThat(second[i]).isEqualTo(first[450 + i]);
        }
    }

    private String buildText(int wordCount) {
        return IntStream.range(0, wordCount)
                .mapToObj(i -> "word" + i)
                .collect(Collectors.joining(" "));
    }
}
