package br.com.docquery.document.document.infrastructure.messaging.config;

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

    public static final String EXCHANGE = "docquery.events";
    public static final String QUEUE = "document.parsed";
    public static final String RETRY_QUEUE = "document.parsed.retry";
    public static final String DLQ = "document.parsed.dlq";
    public static final String ROUTING_KEY = "document.parsed";
    public static final String INDEXED_QUEUE = "document.indexed";
    public static final String INDEXED_ROUTING_KEY = "document.indexed";

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
                .withArgument("x-message-ttl", 30000)
                .build();
    }

    @Bean
    public Queue documentParsedDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding documentParsedBinding() {
        return BindingBuilder
                .bind(documentParsedQueue())
                .to(documentExchange())
                .with(ROUTING_KEY);
    }

    @Bean
    public Queue documentIndexedQueue() {
        return QueueBuilder.durable(INDEXED_QUEUE).build();
    }

    @Bean
    public Binding documentIndexedBinding() {
        return BindingBuilder
                .bind(documentIndexedQueue())
                .to(documentExchange())
                .with(INDEXED_ROUTING_KEY);
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
