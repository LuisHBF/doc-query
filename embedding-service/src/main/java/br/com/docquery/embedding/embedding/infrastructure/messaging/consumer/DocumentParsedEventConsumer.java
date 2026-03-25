package br.com.docquery.embedding.embedding.infrastructure.messaging.consumer;

import br.com.docquery.commons.messaging.DocumentParsedEvent;
import br.com.docquery.embedding.embedding.application.EmbeddingProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentParsedEventConsumer {

    private final EmbeddingProcessingService embeddingProcessingService;

    @RabbitListener(queues = "document.parsed", ackMode = "MANUAL")
    public void consume(DocumentParsedEvent event,
                        Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.info("Received DocumentParsedEvent for document {}", event.getDocumentId());

        try {
            embeddingProcessingService.process(event);
            channel.basicAck(deliveryTag, false);
            log.info("DocumentParsedEvent acknowledged for document {}", event.getDocumentId());
        } catch (Exception e) {
            log.error("Failed to process DocumentParsedEvent for document {}", event.getDocumentId(), e);
            channel.basicNack(deliveryTag, false, true);
        }

    }

}
