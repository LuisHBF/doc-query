package br.com.docquery.embedding.infrastructure.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE    = "docquery.events";
    public static final String QUEUE       = "document.parsed";
    public static final String RETRY_QUEUE = "document.parsed.retry";
    public static final String DLQ         = "document.parsed.dlq";

    private static final int RETRY_TTL_MS = 30_000;

    @Bean
    public TopicExchange documentExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue documentParsedQueue() {
        return QueueBuilder.durable(QUEUE)
                .withArgument("x-dead-letter-exchange", EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RETRY_QUEUE)
                .build();
    }

    @Bean
    public Queue documentParsedRetryQueue() {
        return QueueBuilder.durable(RETRY_QUEUE)
                .withArgument("x-dead-letter-exchange", EXCHANGE)
                .withArgument("x-dead-letter-routing-key", QUEUE)
                .withArgument("x-message-ttl", RETRY_TTL_MS)
                .build();
    }

    @Bean
    public Queue documentParsedDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding documentParsedBinding() {
        return BindingBuilder.bind(documentParsedQueue()).to(documentExchange()).with(QUEUE);
    }

    @Bean
    public Binding documentParsedRetryBinding() {
        return BindingBuilder.bind(documentParsedRetryQueue()).to(documentExchange()).with(RETRY_QUEUE);
    }

    @Bean
    public Binding documentParsedDlqBinding() {
        return BindingBuilder.bind(documentParsedDlq()).to(documentExchange()).with(DLQ);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

}
