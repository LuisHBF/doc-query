package br.com.docquery.embedding.embedding.infrastructure.messaging.publisher;

import br.com.docquery.commons.messaging.DocumentIndexedEvent;
import br.com.docquery.commons.messaging.DocumentIndexingStartedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentEventPublisher {

    private static final String EXCHANGE = "docquery.events";
    private static final String INDEXED_ROUTING_KEY = "document.indexed";
    private static final String INDEXING_STARTED_ROUTING_KEY = "document.indexing.started";

    private final RabbitTemplate rabbitTemplate;

    public void publishDocumentIndexingStarted(DocumentIndexingStartedEvent event) {
        rabbitTemplate.convertAndSend(EXCHANGE, INDEXING_STARTED_ROUTING_KEY, event);
        log.info("Published DocumentIndexingStartedEvent for document {}", event.getDocumentId());
    }

    public void publishDocumentIndexed(DocumentIndexedEvent event) {
        rabbitTemplate.convertAndSend(EXCHANGE, INDEXED_ROUTING_KEY, event);
        log.info("Published DocumentIndexedEvent for document {}", event.getDocumentId());
    }

}
