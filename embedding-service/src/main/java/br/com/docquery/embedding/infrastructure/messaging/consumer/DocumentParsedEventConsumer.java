package br.com.docquery.embedding.infrastructure.messaging.consumer;

import br.com.docquery.commons.messaging.DocumentParsedEvent;
import br.com.docquery.embedding.application.EmbeddingProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentParsedEventConsumer {

    private static final int MAX_RETRIES = 3;
    private static final String EXCHANGE = "docquery.events";
    private static final String DLQ_ROUTING_KEY = "document.parsed.dlq";

    private final EmbeddingProcessingService embeddingProcessingService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = "document.parsed", ackMode = "MANUAL")
    public void consume(DocumentParsedEvent event,
                        Message message,
                        Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.info("Received DocumentParsedEvent for document {}", event.getDocumentId());

        try {
            embeddingProcessingService.process(event);
            channel.basicAck(deliveryTag, false);
            log.info("DocumentParsedEvent acknowledged for document {}", event.getDocumentId());
        } catch (Exception e) {
            int retryCount = getRetryCount(message);
            if (retryCount >= MAX_RETRIES) {
                log.error("Max retries ({}) reached for document {}, routing to DLQ", MAX_RETRIES, event.getDocumentId(), e);
                rabbitTemplate.convertAndSend(EXCHANGE, DLQ_ROUTING_KEY, event);
                channel.basicAck(deliveryTag, false);
            } else {
                log.warn("Processing failed for document {} (attempt {}), scheduling retry", event.getDocumentId(), retryCount + 1, e);
                channel.basicNack(deliveryTag, false, false);
            }
        }
    }

    private int getRetryCount(Message message) {
        List<Map<String, ?>> xDeath = message.getMessageProperties().getXDeathHeader();
        if (xDeath == null || xDeath.isEmpty()) {
            return 0;
        }
        return xDeath.stream()
                .filter(d -> "document.parsed".equals(d.get("queue")))
                .mapToInt(d -> ((Number) d.get("count")).intValue())
                .findFirst()
                .orElse(0);
    }

}
