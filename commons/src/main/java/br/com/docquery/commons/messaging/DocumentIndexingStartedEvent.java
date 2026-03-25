package br.com.docquery.commons.messaging;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class DocumentIndexingStartedEvent {

    UUID documentId;
    UUID userId;

}
