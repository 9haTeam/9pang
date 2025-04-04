package com._hateam.delivery.service;

import com._hateam.common.constant.KafkaTopics;
import com._hateam.common.event.DeliveryCreatedEvent;
import com._hateam.common.event.DeliveryStatusChangedEvent;
import com._hateam.common.event.KafkaEvent;
import com._hateam.delivery.entity.Delivery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryKafkaService {

    private final KafkaTemplate<String, KafkaEvent> kafkaTemplate;

    /**
     * kafka 통한 상태 수정 메세지 - 로그만 남기고 실제 이벤트는 보내지 않음
     */
    public void orderUpdateByKafka(Delivery delivery) {
        DeliveryStatusChangedEvent event = DeliveryStatusChangedEvent.builder()
                .deliveryId(delivery.getId())
                .orderId(delivery.getOrderId())
                .newStatus(delivery.getStatus().name())
                .statusChangedAt(LocalDateTime.now())
                .build();

        // 실제 Kafka 전송은 주석 처리하고 로그만 남김
        log.info("주문상태 업데이트 이벤트(모의): {} - {}", delivery.getId(), delivery.getStatus());
        // kafkaTemplate.send(KafkaTopics.DELIVERY_STATUS_CHANGED, KafkaTopics.DELIVERY_STATUS_CHANGED, event);
    }

    /**
     * kafka 통한 배송 생성 메세지 - 로그만 남기고 실제 이벤트는 보내지 않음
     */
    public void deliveryCreatedByKafka(Delivery delivery) {
        DeliveryCreatedEvent deliveryCreatedEvent = DeliveryCreatedEvent.builder()
                .deliveryId(delivery.getId())
                .orderId(delivery.getOrderId())
                .status(delivery.getStatus().name())
                .build();

        // 실제 Kafka 전송은 주석 처리하고 로그만 남김
        log.info("배송 생성 이벤트(모의): {} - {} - {}",
                delivery.getId(), delivery.getOrderId(), delivery.getStatus());
        // kafkaTemplate.send(KafkaTopics.DELIVERY_CREATED, KafkaTopics.DELIVERY_CREATED, deliveryCreatedEvent);
    }
}