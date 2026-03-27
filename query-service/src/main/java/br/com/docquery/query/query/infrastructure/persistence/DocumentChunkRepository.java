package br.com.docquery.query.query.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DocumentChunkRepository {

    private final JdbcTemplate jdbcTemplate;

    public boolean existsByDocumentIdAndUserId(UUID documentId, UUID userId) {
        String sql = """
                SELECT EXISTS(
                    SELECT 1 FROM document_chunks dc
                    JOIN documents d ON d.id = dc.document_id
                    WHERE dc.document_id = ? AND d.user_id = ?
                )
                """;
        Boolean result = jdbcTemplate.queryForObject(sql, Boolean.class, documentId, userId);
        return Boolean.TRUE.equals(result);
    }

    public List<DocumentChunkEntity> findSimilarChunks(UUID documentId, float[] queryEmbedding, int topK) {
        String vectorLiteral = toVectorLiteral(queryEmbedding);

        String sql = """
                SELECT id, document_id, chunk_index, content, embedding
                FROM document_chunks
                WHERE document_id = ?
                ORDER BY embedding <=> ?::vector
                LIMIT ?
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            DocumentChunkEntity chunk = DocumentChunkEntity.builder()
                    .id(UUID.fromString(rs.getString("id")))
                    .documentId(UUID.fromString(rs.getString("document_id")))
                    .chunkIndex(rs.getInt("chunk_index"))
                    .content(rs.getString("content"))
                    .build();
            return chunk;
        }, documentId, vectorLiteral, topK);
    }

    private String toVectorLiteral(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

}