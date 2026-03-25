package br.com.docquery.commons.messaging;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder
public class DocumentParsedEvent {

    UUID documentId;
    UUID userId;
    List<UUID> chunkIds;

}