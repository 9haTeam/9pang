package com._hateam.order.infrastructure.config;

import com._hateam.common.event.DeliveryCreatedEvent;
import com._hateam.common.event.DeliveryStatusChangedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "order-consumer-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Object.class.getName());
        props.put(JsonDeserializer.TYPE_MAPPINGS,
                "delivery-created:com._hateam.common.event.DeliveryCreatedEvent," +
                        "delivery-status-changed:com._hateam.common.event.DeliveryStatusChangedEvent");

        return new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(),
                new JsonDeserializer<>(Object.class, false));
    }

    @Bean
    public CommonErrorHandler errorHandler() {
        // 에러 발생 시 최대 3번, 5초 간격으로 재시도
        return new DefaultErrorHandler(
                (record, exception) -> {
                    System.err.println("Kafka 메시지 처리 중 오류 발생: " + exception.getMessage());
                    exception.printStackTrace();
                },
                new FixedBackOff(5000L, 3L)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setCommonErrorHandler(errorHandler());
        return factory;
    }

    // DeliveryCreatedEvent 전용 컨테이너 팩토리
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DeliveryCreatedEvent> deliveryCreatedListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, DeliveryCreatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "order-consumer-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        ConsumerFactory<String, DeliveryCreatedEvent> specificConsumerFactory =
                new DefaultKafkaConsumerFactory<>(
                        props,
                        new StringDeserializer(),
                        new JsonDeserializer<>(DeliveryCreatedEvent.class, false));

        factory.setConsumerFactory(specificConsumerFactory);
        factory.setCommonErrorHandler(errorHandler());
        return factory;
    }

    // DeliveryStatusChangedEvent 전용 컨테이너 팩토리
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DeliveryStatusChangedEvent> statusChangedListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, DeliveryStatusChangedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "order-consumer-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        ConsumerFactory<String, DeliveryStatusChangedEvent> specificConsumerFactory =
                new DefaultKafkaConsumerFactory<>(
                        props,
                        new StringDeserializer(),
                        new JsonDeserializer<>(DeliveryStatusChangedEvent.class, false));

        factory.setConsumerFactory(specificConsumerFactory);
        factory.setCommonErrorHandler(errorHandler());
        return factory;
    }
}