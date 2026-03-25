package br.com.docquery.document.document.infrastructure.parsing;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for splitting extracted document text into overlapping chunks
 * suitable for vector embedding and semantic search.
 *
 * <p>Chunking strategy:
 * <ul>
 *   <li>Text is tokenized by whitespace into words</li>
 *   <li>Each chunk contains up to {@value #CHUNK_SIZE} words</li>
 *   <li>Consecutive chunks overlap by {@value #CHUNK_OVERLAP} words to preserve
 *       context at chunk boundaries</li>
 * </ul>
 *
 * <p>Example with CHUNK_SIZE=5 and CHUNK_OVERLAP=2:
 * <pre>
 *   Input:  "the quick brown fox jumps over the lazy dog"
 *   Chunk1: "the quick brown fox jumps"
 *   Chunk2: "fox jumps over the lazy"
 *   Chunk3: "the lazy dog"
 * </pre>
 *
 * <p>The overlap ensures that semantically related content spanning chunk
 * boundaries is captured in at least one chunk during retrieval.
 */
@Component
public class DocumentChunker {

    /**
     * Maximum number of words per chunk.
     * Approximately 600–700 tokens, well within the nomic-embed-text limit of 8192 tokens.
     */
    private static final int CHUNK_SIZE = 500;

    /**
     * Number of words shared between consecutive chunks.
     * Prevents context loss at chunk boundaries.
     */
    private static final int CHUNK_OVERLAP = 50;

    /**
     * Splits the given text into overlapping word-based chunks.
     *
     * @param text the full extracted text of a document
     * @return ordered list of text chunks ready for embedding
     */
    public List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        String[] words = text.split("\\s+");

        int start = 0;

        while (start < words.length) {
            int end = Math.min(start + CHUNK_SIZE, words.length);

            StringBuilder chunk = new StringBuilder();
            for (int i = start; i < end; i++) {
                if (i > start) {
                    chunk.append(" ");
                }
                chunk.append(words[i]);
            }

            chunks.add(chunk.toString());
            start += CHUNK_SIZE - CHUNK_OVERLAP;
        }

        return chunks;
    }

}