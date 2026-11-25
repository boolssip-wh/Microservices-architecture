package com.swu.smartgrade.common.event;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import java.util.Map;

@Configuration
@EnableKafka
@Profile("infra")
public class KafkaEventConfiguration {
    @Bean
    ProducerFactory<String, String> eventProducerFactory(@Value("${KAFKA_BOOTSTRAP_SERVERS:127.0.0.1:9092}") String brokers) {
        return new DefaultKafkaProducerFactory<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.ACKS_CONFIG, "all",
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true,
                ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000));
    }
    @Bean
    KafkaTemplate<String, String> domainEventKafkaTemplate(ProducerFactory<String, String> factory) {
        return new KafkaTemplate<>(factory);
    }
    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            @Value("${KAFKA_BOOTSTRAP_SERVERS:127.0.0.1:9092}") String brokers,
            @Value("${spring.application.name:smartgrade}") String application) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers,
                ConsumerConfig.GROUP_ID_CONFIG, application,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 3_600_000,
                ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1,
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)));
        return factory;
    }
    @Bean
    KafkaAdmin.NewTopics domainEventsTopics() {
        return new KafkaAdmin.NewTopics(java.util.stream.Stream.of("grading.requested", "grading.completed",
                "grading.failed", "report.completed", "report.failed", "review.confirmed")
                .map(topic -> new NewTopic(topic, 1, (short) 1)).toArray(NewTopic[]::new));
    }
}
