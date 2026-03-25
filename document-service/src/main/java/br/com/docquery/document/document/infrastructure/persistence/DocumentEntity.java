package br.com.docquery.document.document.infrastructure.persistence;

import br.com.docquery.document.document.domain.DocumentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DocumentStatus status;

    @Column(name = "chunk_count", nullable = false)
    private Integer chunkCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "indexed_at")
    private LocalDateTime indexedAt;

}