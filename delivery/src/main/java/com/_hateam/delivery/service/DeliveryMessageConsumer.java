package com._hateam.delivery.service;

import com._hateam.common.constant.KafkaTopics;
import com._hateam.common.event.OrderCreatedEvent;
import com._hateam.delivery.entity.Delivery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryMessageConsumer {

    private final DeliveryService deliveryService;
    private final DeliveryKafkaService deliveryKafkaService;

    @KafkaListener(topics = KafkaTopics.ORDER_CREATED, groupId = "delivery-group")
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("주문 생성 이벤트 수신: {}", event);

        try {
            if (event.getOrderId() == null) {
                log.error("주문 생성 이벤트에 주문 ID가 없습니다: {}", event);
                return;
            }

            // 배송 정보 생성
            Delivery delivery = deliveryService.registerDeliveryAuto(event);

            // 중요: 배송 ID가 null이 아닌지 확인
            if (delivery.getId() == null) {
                log.error("생성된 배송에 ID가 없습니다. 주문 ID: {}", event.getOrderId());
                return;
            }

            // 배송 생성 이벤트 명시적 발행 (서비스 내부에서도 이미 발행하지만 확실하게 하기 위해)
            deliveryKafkaService.deliveryCreatedByKafka(delivery);
            log.info("주문-배송 연결 이벤트 발행 완료: 주문 ID={}, 배송 ID={}",
                    event.getOrderId(), delivery.getId());

        } catch(Exception e) {
            log.error("배송 생성 과정에서 오류 발생: {}", e.getMessage(), e);
        }
    }
}