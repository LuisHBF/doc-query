package br.com.docquery.embedding.embedding.infrastructure.messaging.publisher;

import br.com.docquery.commons.messaging.DocumentIndexedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentEventPublisher {

    private static final String EXCHANGE = "docquery.events";
    private static final String ROUTING_KEY = "document.indexed";

    private final RabbitTemplate rabbitTemplate;

    public void publishDocumentIndexed(DocumentIndexedEvent event) {
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event);
        log.info("Published DocumentIndexedEvent for document {}", event.getDocumentId());
    }

}
