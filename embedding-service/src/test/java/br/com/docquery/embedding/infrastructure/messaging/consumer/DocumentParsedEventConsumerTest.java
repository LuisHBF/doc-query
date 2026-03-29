package br.com.docquery.embedding.infrastructure.messaging.consumer;

import br.com.docquery.commons.messaging.DocumentParsedEvent;
import br.com.docquery.embedding.application.EmbeddingProcessingService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentParsedEventConsumerTest {

    @Mock
    private EmbeddingProcessingService embeddingProcessingService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private DocumentParsedEventConsumer consumer;

    @Test
    @DisplayName("acks the message when EmbeddingProcessingService processes the event without throwing")
    void acksMessageOnSuccess() throws Exception {
        DocumentParsedEvent event = buildEvent();
        Channel channel = mock(Channel.class);
        Message message = mock(Message.class);
        long deliveryTag = 1L;

        consumer.consume(event, message, channel, deliveryTag);

        verify(channel).basicAck(deliveryTag, false);
    }

    @Test
    @DisplayName("nacks without requeue when processing fails and retry count is below MAX_RETRIES — message returns to dead-letter exchange for retry")
    void nacksWithoutRequeueOnFirstFailure() throws Exception {
        DocumentParsedEvent event = buildEvent();
        Channel channel = mock(Channel.class);
        Message message = buildMessage(null);
        long deliveryTag = 2L;
        doThrow(new RuntimeException("Embedding model unavailable")).when(embeddingProcessingService).process(event);

        consumer.consume(event, message, channel, deliveryTag);

        verify(channel).basicNack(deliveryTag, false, false);
    }

    @Test
    @DisplayName("routes event to DLQ and acks the message when x-death count reaches MAX_RETRIES to prevent infinite retry loop")
    void routesToDlqAndAcksWhenMaxRetriesReached() throws Exception {
        DocumentParsedEvent event = buildEvent();
        Channel channel = mock(Channel.class);
        Map<String, Object> xDeathEntry = Map.of("queue", "document.parsed", "count", 3L);
        Message message = buildMessage(List.of(xDeathEntry));
        long deliveryTag = 3L;
        doThrow(new RuntimeException("Persistent failure")).when(embeddingProcessingService).process(event);

        consumer.consume(event, message, channel, deliveryTag);

        verify(rabbitTemplate).convertAndSend(eq("docquery.events"), eq("document.parsed.dlq"), eq(event));
        verify(channel).basicAck(deliveryTag, false);
    }

    private DocumentParsedEvent buildEvent() {
        return DocumentParsedEvent.builder()
                .documentId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .chunkIds(List.of(UUID.randomUUID()))
                .build();
    }

    private Message buildMessage(List<Map<String, Object>> xDeath) {
        MessageProperties properties = mock(MessageProperties.class);
        when(properties.getXDeathHeader()).thenReturn(
                xDeath == null ? null : (List) xDeath
        );
        Message message = mock(Message.class);
        when(message.getMessageProperties()).thenReturn(properties);
        return message;
    }
}
