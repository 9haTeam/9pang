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
     * 배송 상태 변경 시 메시지 발행
     */
    public void orderUpdateByKafka(Delivery delivery) {
        DeliveryStatusChangedEvent event = DeliveryStatusChangedEvent.builder()
                .deliveryId(delivery.getId())
                .orderId(delivery.getOrderId())
                .newStatus(delivery.getStatus().name())
                .statusChangedAt(LocalDateTime.now())
                .build();
        kafkaTemplate.send(KafkaTopics.DELIVERY_STATUS_CHANGED, KafkaTopics.DELIVERY_STATUS_CHANGED, event);
        log.info("배송 상태 변경 이벤트 발행 완료: 배송 ID={}, 주문 ID={}, 상태={}",
                delivery.getId(), delivery.getOrderId(), delivery.getStatus());
    }

    /**
     * 배송 생성 시 메시지 발행
     */
    public void deliveryCreatedByKafka(Delivery delivery) {
        DeliveryCreatedEvent deliveryCreatedEvent = DeliveryCreatedEvent.builder()
                .deliveryId(delivery.getId())
                .orderId(delivery.getOrderId())
                .status(delivery.getStatus().name())
                .build();

        kafkaTemplate.send(KafkaTopics.DELIVERY_CREATED, KafkaTopics.DELIVERY_CREATED, deliveryCreatedEvent);
        log.info("배송 생성 이벤트 발행 완료: 배송 ID={}, 주문 ID={}", delivery.getId(), delivery.getOrderId());
    }
}