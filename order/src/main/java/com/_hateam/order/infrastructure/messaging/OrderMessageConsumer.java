package com._hateam.order.infrastructure.messaging;

import com._hateam.common.constant.KafkaTopics;
import com._hateam.common.event.DeliveryCreatedEvent;
import com._hateam.common.event.DeliveryStatusChangedEvent;
import com._hateam.order.application.service.OrderService;
import com._hateam.order.domain.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = "${kafka.topics.delivery-status-changed}")
    public void handleDeliveryStatusChanged(DeliveryStatusChangedEvent event) {
        log.info("배송 상태 변경 이벤트 수신: {}", event);

        try {
            // 배송 상태에 따라 주문 상태 업데이트
            switch (event.getNewStatus()) {
                case "WAITING_AT_HUB":
                    // 배송 전
                    break;
                case "MOVING_TO_HUB":
                case "ARRIVED_AT_DEST_HUB":
                case "OUT_FOR_DELIVERY":
                case "MOVING_TO_COMPANY":
                    // 배송 중
                    orderService.updateOrderStatus(event.getOrderId(), OrderStatus.IN_DELIVERY);
                    break;
                case "DELIVERY_COMPLETED":
                    // 배송 완료
                    orderService.updateOrderStatus(event.getOrderId(), OrderStatus.DONE);
                    break;
                default:
                    log.warn("알 수 없는 배송 상태: {}", event.getNewStatus());
            }
        } catch (Exception e) {
            log.error("배송 상태 변경 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 배송 생성 이벤트를 수신하여 주문에 배송 ID를 업데이트합니다.
     *
     * @param event 배송 생성 이벤트
     */
    @KafkaListener(topics = KafkaTopics.DELIVERY_CREATED)
    public void handleDeliveryCreated(DeliveryCreatedEvent event) {
        log.info("배송 생성 이벤트 수신: {}", event);

        // 필수 정보 확인
        if (event.getOrderId() == null || event.getDeliveryId() == null) {
            log.error("수신된 배송 이벤트에 필수 정보 누락: {}", event);
            return;
        }

        try {
            orderService.updateDeliveryId(event.getOrderId(), event.getDeliveryId());
            log.info("주문 ID: {}에 배송 ID: {} 연결 완료", event.getOrderId(), event.getDeliveryId());
        } catch (Exception e) {
            log.error("배송 생성 이벤트 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}