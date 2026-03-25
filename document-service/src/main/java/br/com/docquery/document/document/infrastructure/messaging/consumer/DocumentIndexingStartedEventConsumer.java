package br.com.docquery.document.document.infrastructure.messaging.consumer;

import br.com.docquery.commons.messaging.DocumentIndexingStartedEvent;
import br.com.docquery.document.document.domain.Document;
import br.com.docquery.document.document.domain.DocumentRepository;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentIndexingStartedEventConsumer {

    private final DocumentRepository documentRepository;

    @RabbitListener(queues = "document.indexing.started", ackMode = "MANUAL")
    public void consume(DocumentIndexingStartedEvent event,
                        Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.info("Received DocumentIndexingStartedEvent for document {}", event.getDocumentId());

        try {
            Document document = documentRepository.findById(event.getDocumentId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Document not found: " + event.getDocumentId()));

            Document indexing = document.startIndexing();

            documentRepository.save(indexing);

            channel.basicAck(deliveryTag, false);

            log.info("Document {} transitioned to INDEXING", event.getDocumentId());
        } catch (Exception e) {
            log.error("Failed to process DocumentIndexingStartedEvent for document {}", event.getDocumentId(), e);
            boolean requeue = !(e instanceof IllegalStateException || e instanceof IllegalArgumentException);
            channel.basicNack(deliveryTag, false, requeue);
        }

    }

}
