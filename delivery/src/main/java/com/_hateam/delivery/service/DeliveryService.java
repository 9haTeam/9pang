package com._hateam.delivery.service;

import com._hateam.common.exception.CustomConflictException;
import com._hateam.common.exception.CustomNotFoundException;
import com._hateam.delivery.dto.request.RegisterDeliveryRequestDto;
import com._hateam.delivery.dto.request.UpdateDeliveryRequestDto;
import com._hateam.delivery.dto.response.*;
import com._hateam.delivery.entity.Delivery;
import com._hateam.delivery.entity.DeliveryRoute;
import com._hateam.delivery.entity.DeliveryStatus;
import com._hateam.delivery.repository.DeliveryRepository;
import com._hateam.delivery.repository.DeliveryRepositoryCustom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryRepositoryCustom deliveryRepositoryCustom;
    private final DeliveryKafkaService deliveryKafkaService;

    /**
     * querydsl 통한 전체 조회
     */
    @Transactional(readOnly = true)
    public Page<DeliveryResponseDto> findDeliveryListForMaster(Pageable pageable) {
        return deliveryRepositoryCustom.findDeliveryListWithPage(pageable);
    }

    /**
     * querydsl 통한 검색
     */
    @Transactional(readOnly = true)
    public Page<DeliveryResponseDto> searchDeliveryListForMaster(
            Pageable pageable,
            DeliveryStatus status,
            String keyword) {
        return deliveryRepositoryCustom.searchDeliveryListWithPage(status, keyword, pageable);
    }

    /**
     * 간소화된 배송 생성 메서드
     */
    @Transactional
    public URI registerDeliveryForMaster(RegisterDeliveryRequestDto registerDeliveryRequestDto) {
        // 이미 존재하는 값인지 확인
        checkDeliveryByOrderId(registerDeliveryRequestDto.getOrderId());

        // 간소화된 객체 생성 (테스트용)
        // 실제 환경에서는 외부 서비스 호출을 통해 데이터를 받아와야 함
        UUID orderId = registerDeliveryRequestDto.getOrderId();

        Delivery delivery = Delivery.builder()
                .orderId(orderId)
                .status(DeliveryStatus.WAITING_AT_HUB)
                .startHubId(UUID.randomUUID())
                .endHubId(UUID.randomUUID())
                .receiverAddress("테스트 주소")
                .receiverName("테스트 수령인")
                .receiverSlackId("테스트슬랙ID")
                .build();

        // 간소화된 배송 경로 생성 (테스트용)
        List<DeliveryRoute> deliveryRouteList = new ArrayList<>();

        // 첫번째 경로 추가
        DeliveryRoute route = DeliveryRoute.builder()
                .delivery(delivery)
                .status(DeliveryStatus.WAITING_AT_HUB)
                .sequence(0)
                .startHubId(UUID.randomUUID())
                .endHubId(UUID.randomUUID())
                .predicDistance(100L)
                .predicTime(120)
                .build();

        deliveryRouteList.add(route);

        delivery.addDeliveyRouteListFrom(deliveryRouteList);

        Delivery savedDelivery = deliveryRepository.save(delivery);

        // Kafka 메시지는 로컬 개발 환경에서 오류 가능성이 있어 주석 처리
        // deliveryKafkaService.deliveryCreatedByKafka(savedDelivery);

        return ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedDelivery.getId())
                .toUri();
    }

    /**
     * order로 부터 message 수신시 배송생성 - 간소화
     */
    public Delivery registerDeliveryAuto(com._hateam.common.event.OrderCreatedEvent event) {
        // 테스트용 간소화된 구현
        Delivery delivery = Delivery.builder()
                .orderId(event.getOrderId())
                .status(DeliveryStatus.WAITING_AT_HUB)
                .startHubId(event.getHubId())
                .endHubId(UUID.randomUUID())
                .receiverAddress("테스트 주소")
                .receiverName("테스트 수령인")
                .receiverSlackId("테스트슬랙ID")
                .build();

        List<DeliveryRoute> deliveryRouteList = new ArrayList<>();
        DeliveryRoute route = DeliveryRoute.builder()
                .delivery(delivery)
                .status(DeliveryStatus.WAITING_AT_HUB)
                .sequence(0)
                .startHubId(event.getHubId())
                .endHubId(UUID.randomUUID())
                .predicDistance(100L)
                .predicTime(120)
                .build();

        deliveryRouteList.add(route);

        delivery.addDeliveyRouteListFrom(deliveryRouteList);

        return deliveryRepository.save(delivery);
    }

    /**
     * 배송정보 상세 조회
     */
    @Transactional(readOnly = true)
    public DeliveryResponseDto getDeliveryForMaster(UUID deliveryId) {
        Delivery delivery = checkDelivery(deliveryId);
        return DeliveryResponseDto.from(delivery);
    }

    /**
     * 배송정보 수정
     */
    @Transactional
    public URI updateDeliveryForMaster(UUID deliveryId, UpdateDeliveryRequestDto updateDeliveryRequestDto) {
        Delivery delivery = checkDelivery(deliveryId);
        delivery.updateOf(updateDeliveryRequestDto);

        return ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(delivery.getId())
                .toUri();
    }

    /**
     * 배송 상태 수정 - 간소화
     */
    @Transactional
    public URI updateDeliveryStatus(UUID deliveryId, DeliveryStatus status) {
        Delivery delivery = checkDelivery(deliveryId);
        delivery.updateStatusOf(status);

        // Kafka 이벤트 발행 로직 주석 처리 (로컬 개발 환경에서 오류 방지)
        // if (status == DeliveryStatus.DELIVERY_COMPLETED) {
        //     deliveryKafkaService.orderUpdateByKafka(delivery);
        // }

        return ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(delivery.getId())
                .toUri();
    }

    /**
     * 배송정보 삭제
     */
    @Transactional
    public void deleteDeliveryForMaster(UUID deliveryId) {
        Delivery delivery = checkDelivery(deliveryId);
        delivery.deleteOf("deleter"); // 실제 환경에서는 인증된 사용자 정보 필요
    }

    /*내부 메서드------------------------------------------------------------------------------------------------------*/

    /**
     * 조회시 check사항
     */
    private Delivery checkDelivery(final UUID deliveryId) {
        return deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new CustomNotFoundException("존재하지 않는 배송정보입니다."));
    }

    /**
     * orderId를 통한 delivery 확인
     */
    private void checkDeliveryByOrderId(final UUID orderId) {
        boolean deliveryIsExists = deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId);
        if (deliveryIsExists) {
            throw new CustomConflictException("주어진 주문에 대한 배송정보는 이미 존재합니다.");
        }
    }
}