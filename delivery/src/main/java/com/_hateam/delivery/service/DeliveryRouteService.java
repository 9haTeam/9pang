package com._hateam.delivery.service;

import com._hateam.common.exception.CustomNotFoundException;
import com._hateam.delivery.dto.request.UpdateDeliveryRouteRequestDto;
import com._hateam.delivery.dto.response.DeiiveryRouteResponseDto;
import com._hateam.delivery.entity.Delivery;
import com._hateam.delivery.entity.DeliveryRoute;
import com._hateam.delivery.entity.DeliveryStatus;
import com._hateam.delivery.repository.DeliveryRouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryRouteService {

    private final DeliveryRouteRepository deliveryRouteRepository;
    private final DeliveryKafkaService deliveryKafkaService;

    /**
     * 배송경로 조회 - 단순화
     */
    @Transactional(readOnly = true)
    public DeiiveryRouteResponseDto getDeliveryRouteForMaster(UUID deliveryRouteId) {
        DeliveryRoute deliveryRoute = checkDeliveryRoute(deliveryRouteId);
        return DeiiveryRouteResponseDto.from(deliveryRoute);
    }

    /**
     * 배송경로 수정 - 단순화
     */
    @Transactional
    public URI updateDeliveryRouteForMaster(UUID deliveryRouteId, UpdateDeliveryRouteRequestDto updateDeliveryRouteRequestDto) {
        // 배송 경로 상태 변경
        DeliveryRoute deliveryRoute = checkDeliveryRoute(deliveryRouteId);
        deliveryRoute.updateStatusOf(updateDeliveryRouteRequestDto);

        // 간소화된 상태 업데이트 로직
        DeliveryStatus status = updateDeliveryRouteRequestDto.getStatus();
        Delivery delivery = deliveryRoute.getDelivery();

        if (status != null) {
            // 배송 경로 상태가 변경되면 배송 전체 상태도 업데이트
            log.info("배송 경로 상태 변경: {} -> {}", deliveryRoute.getId(), status);

            // 배송 상태도 같이 업데이트 (실제 환경에서는 조건부 처리 필요)
            delivery.updateStatusOf(status);

            // 경로가 목적지에 도착했다면 업체 배송 담당자 배정 로직 필요 (외부 서비스 연동 필요)
            if (status == DeliveryStatus.ARRIVED_AT_DEST_HUB) {
                log.info("목적지 허브 도착: {}. 업체 배송 담당자 배정 필요", deliveryRoute.getId());
                // 실제 환경에서는 외부 서비스 연동 필요
            }
        }

        return ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(deliveryRoute.getId())
                .toUri();
    }

    /**
     * 배송경로 삭제 - 단순화
     */
    @Transactional
    public void deleteDeliveryRouteForMaster(UUID deliveryRouteId) {
        DeliveryRoute deliveryRoute = checkDeliveryRoute(deliveryRouteId);
        deliveryRoute.deleteOf("deleter"); // 실제 환경에서는 인증된 사용자 정보 필요
        log.info("배송 경로 삭제 완료: {}", deliveryRouteId);
    }

    /*내부 메서드------------------------------------------------------------------------------------------------------*/

    /**
     * 조회시 check사항
     */
    private DeliveryRoute checkDeliveryRoute(final UUID deliveryRouteId) {
        return deliveryRouteRepository.findByIdAndDeletedAtIsNull(deliveryRouteId)
                .orElseThrow(() -> new CustomNotFoundException("존재하지 않는 배송경로정보입니다."));
    }
}