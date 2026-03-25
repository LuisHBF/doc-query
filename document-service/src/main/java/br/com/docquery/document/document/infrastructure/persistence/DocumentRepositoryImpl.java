package br.com.docquery.document.document.infrastructure.persistence;

import br.com.docquery.document.document.domain.Document;
import br.com.docquery.document.document.domain.DocumentRepository;
import br.com.docquery.document.document.domain.state.DocumentState;
import br.com.docquery.document.document.domain.state.UploadedState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DocumentRepositoryImpl implements DocumentRepository {

    private final DocumentJpaRepository documentJpaRepository;

    @Override
    public UUID save(Document document) {
        DocumentEntity entity = DocumentEntity.builder()
                .id(document.getId())
                .userId(document.getUserId())
                .fileName(document.getFileName())
                .fileSizeBytes(document.getFileSizeBytes())
                .mimeType(document.getMimeType())
                .status(document.getStatus())
                .chunkCount(document.getChunkCount())
                .createdAt(document.getCreatedAt())
                .indexedAt(document.getIndexedAt())
                .build();

        DocumentEntity saved = documentJpaRepository.save(entity);
        return saved.getId();
    }

    @Override
    public Optional<Document> findById(UUID id) {
        return documentJpaRepository.findById(id)
                .map(entity -> Document.builder()
                        .id(entity.getId())
                        .userId(entity.getUserId())
                        .fileName(entity.getFileName())
                        .fileSizeBytes(entity.getFileSizeBytes())
                        .mimeType(entity.getMimeType())
                        .state(DocumentState.from(entity.getStatus()))
                        .chunkCount(entity.getChunkCount())
                        .createdAt(entity.getCreatedAt())
                        .indexedAt(entity.getIndexedAt())
                        .build());
    }

    @Override
    public List<Document> findAllByUserId(UUID userId) {
        return documentJpaRepository.findAllByUserId(userId)
                .stream()
                .map(entity -> Document.builder()
                        .id(entity.getId())
                        .userId(entity.getUserId())
                        .fileName(entity.getFileName())
                        .fileSizeBytes(entity.getFileSizeBytes())
                        .mimeType(entity.getMimeType())
                        .state(DocumentState.from(entity.getStatus()))
                        .chunkCount(entity.getChunkCount())
                        .createdAt(entity.getCreatedAt())
                        .indexedAt(entity.getIndexedAt())
                        .build())
                .toList();
    }

    @Override
    public void delete(UUID id) {
        documentJpaRepository.deleteById(id);
    }

}