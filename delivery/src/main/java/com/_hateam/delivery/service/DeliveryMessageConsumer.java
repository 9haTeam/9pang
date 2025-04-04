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
            // 배송 정보 생성
            var delivery = deliveryService.registerDeliveryAuto(event);

            // 중요: 배송 ID가 null이 아닌지 확인
            if (delivery.getId() == null) {
                log.error("생성된 배송에 ID가 없습니다. 주문 ID: {}", event.getOrderId());
                return;
            }

            // 배송 생성 이벤트 발행
            deliveryKafkaService.deliveryCreatedByKafka(delivery);

            log.info("배송 정보 생성 및 이벤트 발행 완료: 주문 ID={}, 배송 ID={}",
                    event.getOrderId(), delivery.getId());
        } catch(Exception e) {
            log.error("배송 생성 과정에서 오류 발생: {}", e.getMessage(), e);
        }
    }
}