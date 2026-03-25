package br.com.docquery.document.document.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentChunkJpaRepository extends JpaRepository<DocumentChunkEntity, UUID> {
}