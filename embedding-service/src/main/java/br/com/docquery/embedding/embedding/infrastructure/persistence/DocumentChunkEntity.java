package br.com.docquery.embedding.embedding.infrastructure.persistence;

import io.hypersistence.utils.hibernate.type.array.FloatArrayType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_chunks")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunkEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "document_id", nullable = false, updatable = false)
    private UUID documentId;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "token_count", nullable = false)
    private Integer tokenCount;

    @Type(FloatArrayType.class)
    @Column(name = "embedding", columnDefinition = "vector(768)")
    private float[] embedding;

    @Column(name = "embedded_at")
    private LocalDateTime embeddedAt;

    public DocumentChunkEntity applyEmbedding(float[] embedding) {
        return DocumentChunkEntity.builder()
                .id(this.id)
                .documentId(this.documentId)
                .chunkIndex(this.chunkIndex)
                .content(this.content)
                .tokenCount(this.tokenCount)
                .embedding(embedding)
                .embeddedAt(LocalDateTime.now())
                .build();
    }

}