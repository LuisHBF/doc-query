package br.com.docquery.document.document.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository {

    UUID save(Document document);

    Optional<Document> findById(UUID id);

    List<Document> findAllByUserId(UUID userId);

    void delete(UUID id);

}