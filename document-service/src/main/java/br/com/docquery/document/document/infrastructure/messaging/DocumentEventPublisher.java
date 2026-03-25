package br.com.docquery.document.document.infrastructure.messaging;

import br.com.docquery.commons.messaging.DocumentParsedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentEventPublisher {

    private static final String EXCHANGE = "docquery.events";
    private static final String ROUTING_KEY = "document.parsed";

    private final RabbitTemplate rabbitTemplate;

    public void publishDocumentParsed(DocumentParsedEvent event) {
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event);
        log.info("Published DocumentParsedEvent for document {}", event.getDocumentId());
    }

}