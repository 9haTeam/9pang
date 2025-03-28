package com._hateam.delivery.service;

import com._hateam.common.constant.KafkaTopics;
import com._hateam.common.dto.ResponseDto;
import com._hateam.common.event.DeliveryStatusChangedEvent;
import com._hateam.common.event.KafkaEvent;
import com._hateam.common.event.OrderCreatedEvent;
import com._hateam.common.exception.CustomConflictException;
import com._hateam.common.exception.CustomNotFoundException;
import com._hateam.delivery.dto.request.RegisterDeliveryRequestDto;
import com._hateam.delivery.dto.request.UpdateDeliveryRequestDto;
import com._hateam.delivery.dto.response.*;
import com._hateam.delivery.entity.Delivery;
import com._hateam.delivery.entity.DeliveryRoute;
import com._hateam.delivery.entity.DeliveryStatus;
import com._hateam.delivery.entity.OrderStatus;
import com._hateam.delivery.feignClient.CompanyClient;
import com._hateam.delivery.feignClient.HubClient;
import com._hateam.delivery.feignClient.UserClient;
import com._hateam.delivery.repository.DeliveryRepository;
import com._hateam.delivery.repository.DeliveryRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryRepositoryCustom deliveryRepositoryCustom;
    private final CompanyClient companyClient;
    private final UserClient userClient;
    private final HubClient hubClient;
    private final DeliveryKafkaService deliveryKafkaService;

    /**
     * querydsl 통한 전체 조회
     */
    // todo: 전체 조회 or 검색시 response를 상세때와 동일하게 둘 필요가 있는가?
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
     * todo: order로 부터 가져올것 : 허브id, 회사id, 주문요청(현재 테이블이나 entity에는 없다),
     * todo: 배송마감(어느 배송마감인지 자세한 정보 확인), 상태(현재 배송준비가 다 되었는지)
     * todo: companyid를 통해 회사의 주소값(수령인 주소), 수령인명, 수령인 slackid
     * todo: 수령인 주소를 통해 가장 가까운 허브의 위치 알아내서 최종목적지hubid 확인
     * todo: 최종목적지hubid를 통해 각허브경로 가져오기 + 그 경로대로 허브 경로 기록 생성
     * todo: 각 경로 기록마다의 예상이동거리, 시간 확인, 배송상태 기본 전부 대기중으로 생성 예상이동거리, 시간의 경우 허브경로쪽에서 가지고 있겠지...?
     * todo: 1번 배송담당자가 전부 부담 -> 돌아가면서 부담 -> 정해진 시간에 배송상태들을 확인하여 같은 hub 끼리 묶어서 배정
     * todo: 필요시 상태값을 좀더 자세히 표현할 필요가 있음... 허브에 들어와서 대기중인지, 아직 허브까지 배송이 안된건지
     * todo: transaction 과정에서 에러 발생으로 배송이 생성되지 않을때의 처리(주문 삭제 요청 or 다른 처리 방법)
     */
    @Transactional
    public URI registerDeliveryForMaster(RegisterDeliveryRequestDto registerDeliveryRequestDto) {
        // 이미 존재하는 값인지 확인
        checkDeliveryByOrderId(registerDeliveryRequestDto.getOrderId());
        // todo: order 조회 구현, 임시로 orderResponse 구성, 별도로 분리하는 쪽이 좋을듯 order로 부터 요청값으로 받을 수도 있다 보류
        OrderClientResponseDto orderClientResponseDto = new OrderClientResponseDto();
        orderClientResponseDto.setOrderId(registerDeliveryRequestDto.getOrderId());
        orderClientResponseDto.setHubId(UUID.randomUUID());
        orderClientResponseDto.setCompanyId(UUID.randomUUID());
        orderClientResponseDto.setOrderRequest("임시 요청입니다.");
        orderClientResponseDto.setOrderStatus(OrderStatus.WAITING);

        // todo: company조회 구현, 임시 companyResponse 구성, 별도로 분리하는 쪽이 좋을듯
        CompanyClientResponseDto companyClientResponseDto = new CompanyClientResponseDto();
        companyClientResponseDto.setCompanyId(orderClientResponseDto.getCompanyId());
        companyClientResponseDto.setCompanyAddress("임시 주소입니다.");
        companyClientResponseDto.setUsername("tester");
//        CompanyClientResponseDto companyClientResponseDto = companyClient
//                .getCompanyById(orderClientResponseDto.getCompanyId())
//                .getBody()
//                .getData();
        // todo: user조회 구현, 임시 user 구성, 별도로 분리하는 쪽이 좋을듯
        UserClientResponseDto userClientResponseDto = new UserClientResponseDto();
        userClientResponseDto.setUsername("tester");
        userClientResponseDto.setSlackId("testerSlack");
        userClientResponseDto.setUsernickname("테스트용");
//        UserClientResponseDto userClientResponseDto = userClient
//                .getUserByUsername(companyClientResponseDto.getUsername())
//                .getBody()
//                .getData();

        // todo: 현재 hub 기준 가장 가까운 허브 위치 찾기, 모든 주소를 저장하는 시점에 위 경도도 같이 저장될거라 우선 가정
        // todo: 그 위치기반으로 주변 위치 찾는 query가 있었는데... 그걸로 찾는다고 가정하고
//        HubClientHubResponseDto hubClientHubResponseDto = new HubClientHubResponseDto();
//        hubClientHubResponseDto.setId(UUID.randomUUID());
//        hubClientHubResponseDto.setName("경기북부");

        HubClientHubResponseDto hubClientHubResponseDto = hubClient
                .getNearestHub(34.8492021, 126.4715102)
                .getBody()
                .getData();

        System.out.println(hubClientHubResponseDto.getName());

        UUID destHubId = hubClientHubResponseDto.getId();

        Delivery delivery = Delivery.addOf(orderClientResponseDto, companyClientResponseDto, userClientResponseDto, destHubId);

        // todo: sequence 확인 + 배송경로 생성
        // todo: sequence 조회
        String sequence =
                "b22ec476-39f7-49c3-8760-22cf3bd04d68, b22ec476-39f7-78c3-8760-22cf3bd04d43, b22ec476-39f0-49c3-8760-22cf3bd04d43";
        // todo: 조회된 sequence의 각 배송경로 생성
        // todo: 생성시점에서 배송경로에 맞는 배송 담당자 배정? 단순하게는 그냥 순번대로 배정
        //  -> 매일 각 허브에서 현재 본인 허브에 도착한 배송을 조회하여 도착허브에 따라 분류하여 배정
        List<DeliveryRoute> deliveryRouteList = new ArrayList<>();
        String[] sequnceArray = sequence.split(", ");
        for (int i = 0; i<sequnceArray.length-1; i++) {
            UUID startHubId = UUID.fromString(sequnceArray[i]);
            UUID endHubId = UUID.fromString(sequnceArray[i+1]);
            HubClientResponseDto hubClientResponseDto = new HubClientResponseDto();
            hubClientResponseDto.setSequence(i);
            hubClientResponseDto.setDistanceKm((long) 25.51);
            hubClientResponseDto.setEstimatedTimeMinutes(240);
            DeliveryRoute deliveryRoute = DeliveryRoute.addOf(delivery, startHubId, endHubId, hubClientResponseDto);
            // 첫배송담당자 배정
            if (i == 0) {
                UserClientDeliverResponseDto userClientDeliverResponseDto = new UserClientDeliverResponseDto();
                userClientDeliverResponseDto.setDeliverId(UUID.randomUUID());
                userClientDeliverResponseDto.setSlackId("slackForFirst");
                deliveryRoute.updateDeliver(userClientDeliverResponseDto);
            }

            deliveryRouteList.add(deliveryRoute);
        }

        delivery.addDeliveyRouteListFrom(deliveryRouteList);


        deliveryRepository.save(delivery);

        return ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(delivery.getId())
                .toUri();
    }

    /**
     * order로 부터 message 수신시 배송생성
     */
    public Delivery registerDeliveryAuto(OrderCreatedEvent event) {
        // 이미 존재하는 값인지 확인
        checkDeliveryByOrderId(event.getOrderId());
        // 받아온 order로 회사 조회
        CompanyClientResponseDto companyClientResponseDto = companyClient
                .getCompanyById(event.getCompanyId())
                .getBody()
                .getData();
        // company로 user 조회
        UserClientResponseDto userClientResponseDto = userClient
                .getUserByUsername(companyClientResponseDto.getUsername())
                .getBody()
                .getData();
        // company의 위도 경도 값을 통해 가장 근처 hub 탐색
        // todo: company에  위도경도 추가
        HubClientHubResponseDto hubClientHubResponseDto = hubClient
                .getNearestHub(34.8492021, 126.4715102)
                .getBody()
                .getData();

        UUID destHubId = hubClientHubResponseDto.getId();

        Delivery delivery = Delivery.addOf(event, companyClientResponseDto, userClientResponseDto, destHubId);

        // todo: sequence 확인 + 배송경로 생성
        // todo: sequence 조회
        String sequence =
                "b22ec476-39f7-49c3-8760-22cf3bd04d68, b22ec476-39f7-78c3-8760-22cf3bd04d43, b22ec476-39f0-49c3-8760-22cf3bd04d43";
        // todo: 조회된 sequence의 각 배송경로 생성
        // todo: 생성시점에서 배송경로에 맞는 배송 담당자 배정? 단순하게는 그냥 순번대로 배정
        //  -> 매일 각 허브에서 현재 본인 허브에 도착한 배송을 조회하여 도착허브에 따라 분류하여 배정
        List<DeliveryRoute> deliveryRouteList = new ArrayList<>();
        String[] sequnceArray = sequence.split(", ");
        for (int i = 0; i<sequnceArray.length-1; i++) {
            UUID startHubId = UUID.fromString(sequnceArray[i]);
            UUID endHubId = UUID.fromString(sequnceArray[i+1]);
            HubClientResponseDto hubClientResponseDto = new HubClientResponseDto();
            hubClientResponseDto.setSequence(i);
            hubClientResponseDto.setDistanceKm((long) 25.51);
            hubClientResponseDto.setEstimatedTimeMinutes(240);
            DeliveryRoute deliveryRoute = DeliveryRoute.addOf(delivery, startHubId, endHubId, hubClientResponseDto);
            // 첫배송담당자 배정
            if (i == 0) {
                // hub 배송자 순번 확인
                UserClientDeliverResponseDto userClientDeliverResponseDto = userClient
                        .getDeliverAssign("DELIVER_HUB", null)
                        .getBody()
                        .getData();
                deliveryRoute.updateDeliver(userClientDeliverResponseDto);
            }

            deliveryRouteList.add(deliveryRoute);
        }

        delivery.addDeliveyRouteListFrom(deliveryRouteList);


        deliveryRepository.save(delivery);

        return delivery;
    }

    /**
     * 배송정보 상세 조회
     * todo: deliveryRoute가 추가되면서 n+1 발생가능, querydsl로 join 처리 필요
     */
    @Transactional(readOnly = true)
    public DeliveryResponseDto getDeliveryForMaster(UUID deliveryId) {
        Delivery delivery = checkDelivery(deliveryId);
        return DeliveryResponseDto.from(delivery);
    }

    /**
     * 배송정보 수정
     * todo: 배송정보에서 status 수정시 order에도 수정사항을 알려줘야 함(mq)
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
     * 배송정보 수정
     * todo: 배송정보에서 status 수정시 order에도 수정사항을 알려줘야 함(mq)
     */
    @Transactional
    public URI updateDeliveryStatus(UUID deliveryId, DeliveryStatus status) {
        Delivery delivery = checkDelivery(deliveryId);

        delivery.updateStatusOf(status);

        if (status == DeliveryStatus.DELIVERY_COMPLETED) {
            deliveryKafkaService.orderUpdateByKafka(delivery);
        }

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
        delivery.deleteOf("deleter"); // todo: 추후에 로그인한 사람으로 수정 필요 생성, 수정자도 관련 로직 필요
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
