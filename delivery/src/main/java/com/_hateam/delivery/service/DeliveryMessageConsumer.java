package com._hateam.delivery.service;

import com._hateam.common.constant.KafkaTopics;
import com._hateam.common.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryMessageConsumer {

    private final DeliveryService deliveryService;
    private final DeliveryKafkaService deliveryKafkaService;

    @KafkaListener(topics = KafkaTopics.ORDER_CREATED, groupId = "delivery_group")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("주문 생성 이벤트 수신: {}", event);

        try {
            // 배송 정보 자동 생성
            log.info("배송 정보 생성 시작: 주문 ID={}", event.getOrderId());

            // 배송 정보 생성
            var delivery = deliveryService.registerDeliveryAuto(event);

            // 배송 생성 이벤트 발행 - 주문 서비스에 배송 ID 전달
            deliveryKafkaService.deliveryCreatedByKafka(delivery);

            log.info("배송 정보 생성 및 이벤트 발행 완료: 주문 ID={}, 배송 ID={}",
                    event.getOrderId(), delivery.getId());

        } catch(Exception e) {
            // 실패에 대한 보상처리 - 로깅 강화
            log.error("배송 생성 과정에서 오류 발생: 주문 ID={}, 오류={}",
                    event.getOrderId(), e.getMessage(), e);

            // TODO: 보상 트랜잭션 또는 실패 이벤트 발행 로직 추가
        }
    }
}