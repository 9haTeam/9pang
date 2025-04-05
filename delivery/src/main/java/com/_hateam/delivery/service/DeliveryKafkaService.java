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
        try {
            DeliveryStatusChangedEvent event = DeliveryStatusChangedEvent.builder()
                    .deliveryId(delivery.getId())
                    .orderId(delivery.getOrderId())
                    .newStatus(delivery.getStatus().name())
                    .statusChangedAt(LocalDateTime.now())
                    .build();

            log.info("배송 상태 변경 이벤트 발행 시작: {}", event);
            kafkaTemplate.send(KafkaTopics.DELIVERY_STATUS_CHANGED, KafkaTopics.DELIVERY_STATUS_CHANGED, event);
            log.info("배송 상태 변경 이벤트 발행 완료: 배송 ID={}, 주문 ID={}, 상태={}",
                    delivery.getId(), delivery.getOrderId(), delivery.getStatus());
        } catch (Exception e) {
            log.error("배송 상태 변경 이벤트 발행 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 배송 생성 시 메시지 발행
     */
    public void deliveryCreatedByKafka(Delivery delivery) {
        try {
            // 필수 정보 확인
            if (delivery.getId() == null || delivery.getOrderId() == null) {
                log.error("배송 이벤트 발행 실패: 필수 ID 없음. 배송 ID={}, 주문 ID={}",
                        delivery.getId(), delivery.getOrderId());
                return;
            }

            DeliveryCreatedEvent deliveryCreatedEvent = DeliveryCreatedEvent.builder()
                    .deliveryId(delivery.getId())
                    .orderId(delivery.getOrderId())
                    .status(delivery.getStatus().name())
                    .build();

            // 디버그용 로그 추가
            log.info("배송 생성 이벤트 내용: {}", deliveryCreatedEvent);

            kafkaTemplate.send(KafkaTopics.DELIVERY_CREATED, KafkaTopics.DELIVERY_CREATED, deliveryCreatedEvent);
            log.info("배송 생성 이벤트 발행 완료: 배송 ID={}, 주문 ID={}", delivery.getId(), delivery.getOrderId());
        } catch (Exception e) {
            log.error("배송 생성 이벤트 발행 실패: {}", e.getMessage(), e);
        }
    }
}