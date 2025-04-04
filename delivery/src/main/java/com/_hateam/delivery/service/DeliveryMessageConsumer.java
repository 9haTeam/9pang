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

    // 주석 처리 - 로컬 개발 환경에서는 이 기능 비활성화
    /*
    @KafkaListener(topics = KafkaTopics.ORDER_CREATED, groupId = "delivery_group")
    public void handleDeliveryStatusChanged(OrderCreatedEvent event) {
        log.info("주문생성 이벤트 수신: {}", event);

        try {
            // 배송 서비스를 통해 주문 생성 처리
            deliveryService.registerDeliveryAuto(event);

        } catch(Exception e) {
            log.error("배송 생성과정에서 오류 발생: {}", e.getMessage(), e);
        }
    }
    */

    // 테스트용 메서드 추가
    public void testHandleDeliveryEvent(OrderCreatedEvent event) {
        log.info("테스트 주문생성 이벤트 처리: {}", event);
        try {
            deliveryService.registerDeliveryAuto(event);
            log.info("테스트 배송 생성 성공");
        } catch(Exception e) {
            log.error("테스트 배송 생성과정에서 오류 발생: {}", e.getMessage(), e);
        }
    }
}