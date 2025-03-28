package com._hateam.order.application.service;

import com._hateam.common.constant.KafkaTopics;
import com._hateam.common.event.OrderCreatedEvent;
import com._hateam.common.event.OrderCreatedForSlackEvent;
import com._hateam.common.exception.CustomConflictException;
import com._hateam.common.exception.CustomNotFoundException;
import com._hateam.order.application.dto.OrderRequestDto;
import com._hateam.order.application.dto.OrderResponseDto;
import com._hateam.order.application.dto.OrderSearchDto;
import com._hateam.order.application.dto.OrderUpdateDto;
import com._hateam.order.domain.model.Order;
import com._hateam.order.domain.model.OrderProduct;
import com._hateam.order.domain.model.OrderStatus;
import com._hateam.order.domain.repository.OrderRepository;
import com._hateam.order.domain.service.OrderDomainService;
import com._hateam.order.infrastructure.client.CompanyClient;
import com._hateam.order.infrastructure.client.DeliveryClient;
import com._hateam.order.infrastructure.client.HubClient;
import com._hateam.order.infrastructure.client.UserClient;
import com._hateam.order.infrastructure.client.dto.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderDomainService orderDomainService;
    private final CompanyClient companyClient;
    private final DeliveryClient deliveryClient;
    private final HubClient hubClient;
    private final UserClient userClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 새로운 주문을 생성합니다.
     *
     * @param requestDto 주문 생성 요청 DTO
     * @return 생성된 주문 정보 응답 DTO
     * @throws CustomConflictException  재고가 부족하거나 서비스 연동 중 에러 발생 시
     * @throws IllegalArgumentException 요청 데이터가 유효하지 않은 경우
     */
    @Transactional
    public OrderResponseDto createOrder(OrderRequestDto requestDto) {
        Order order = orderDomainService.createOrder(
                null, // deliveryId를 null로 설정
                requestDto.getHubId(),
                requestDto.getCompanyId(),
                requestDto.getOrderRequest(),
                requestDto.getDeliveryDeadline()
        );

        order.setTotalPrice(0);

        // 주문 상품 추가 및 재고 확인/감소
        for (OrderRequestDto.OrderProductDto productDto : requestDto.getProducts()) {
            // 상품 정보 조회
            ProductDto product;
            try {
                product = companyClient.getProductById(productDto.getProductId()).getData();
            } catch (Exception e) {
                throw new CustomConflictException("상품 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
            }

            // 재고 확인
            if (product.getQuantity() < productDto.getQuantity()) {
                throw new CustomConflictException("상품 재고가 부족합니다. 상품: " + product.getName() +
                        ", 현재 재고: " + product.getQuantity() + ", 요청 수량: " + productDto.getQuantity());
            }

            // 주문 상품 생성
            OrderProduct orderProduct = orderDomainService.createOrderProduct(
                    productDto.getProductId(),
                    productDto.getQuantity(),
                    productDto.getTotalPrice() / productDto.getQuantity() // 단가 계산
            );
            order.addOrderProduct(orderProduct);

            // 재고 감소
            try {
                ProductRequestDto updateRequest = ProductRequestDto.builder()
                        .companyId(product.getCompanyId())
                        .name(product.getName())
                        .description(product.getDescription())
                        .price(product.getPrice())
                        .quantity(product.getQuantity() - productDto.getQuantity())
                        .build();

                companyClient.updateProduct(productDto.getProductId(), updateRequest);
            } catch (Exception e) {
                throw new CustomConflictException("상품 재고 업데이트 중 오류가 발생했습니다: " + e.getMessage());
            }
        }

        // 총 가격 계산
        order.calculateTotalPrice();

        Order savedOrder = orderRepository.save(order);

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(savedOrder.getOrderId())
                .hubId(savedOrder.getHubId())
                .companyId(savedOrder.getCompanyId())
                .orderRequest(savedOrder.getOrderRequest())
                .deliveryDeadline(savedOrder.getDeliveryDeadline())
                .build();

        kafkaTemplate.send(KafkaTopics.ORDER_CREATED, event);

        sendOrderCreatedSlackNotification(savedOrder);

        return OrderResponseDto.from(savedOrder);
    }

    // 슬랙 알림을 위한 이벤트 발행 메서드
    private void sendOrderCreatedSlackNotification(Order order) {
        try {
            // 업체 정보 조회
            CompanyDto company = companyClient.getCompanyById(order.getCompanyId()).getData();

            // 상품 정보 조회 - 첫 번째 상품만 예시로 표시
            OrderProduct firstProduct = order.getOrderProducts().get(0);
            ProductDto product = companyClient.getProductById(firstProduct.getProductId()).getData();

            // 배송 경로 정보 조회
            DeliveryDto delivery = deliveryClient.getDelivery(order.getDeliverId()).getData();

            // 허브 정보 조회
            HubDto startHub = hubClient.getHubById(delivery.getStartHubId()).getData();

            // 허브 경로 정보 조회
            List<String> viaHubs = new ArrayList<>();
            // TODO: 배송 경로 정보를 조회하여 허브 경로 목록 설정
            viaHubs.add("대전광역시 센터");
            viaHubs.add("부산광역시 센터");

            // 배송 담당자 정보 조회
            DeliverUserDto deliverer = userClient.getDeliverUserById(delivery.getDelivererId()).getData();

            OrderCreatedForSlackEvent event = OrderCreatedForSlackEvent.builder()
                    .orderId(order.getOrderId())
                    .orderNumber(order.getOrderId().toString().substring(0, 8)) // 간략화된 주문번호
                    .customerName(company.getCompanyName())
                    .customerEmail("example@company.com") // 예시 이메일
                    .productInfo(product.getName() + " " + firstProduct.getTotalQuantity() + "박스")
                    .requestInfo(order.getOrderRequest())
                    .startHub(startHub.getName())
                    .viaHubs(viaHubs)
                    .destination(delivery.getReceiverAddress())
                    .delivererName(deliverer.getName())
                    .delivererSlackId(deliverer.getSlackId())
                    .deliveryDeadline(order.getDeliveryDeadline())
                    .build();

            kafkaTemplate.send(KafkaTopics.ORDER_CREATED_FOR_SLACK, event);

            log.info("주문 생성 슬랙 알림 이벤트 발행 완료. 주문 ID: {}", order.getOrderId());
        } catch (Exception e) {
            log.error("주문 생성 슬랙 알림 이벤트 발행 중 오류 발생: {}", e.getMessage(), e);
            // 주문 처리는 계속 진행하고 알림만 실패하도록 예외를 던지지 않음
        }
    }

    /**
     * 주문 ID로 주문을 조회합니다.
     *
     * @param orderId 조회할 주문 ID
     * @return 주문 정보 응답 DTO
     * @throws CustomNotFoundException 주문이 존재하지 않는 경우
     */
    public OrderResponseDto getOrderById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomNotFoundException("주문을 찾을 수 없습니다. ID: " + orderId));

        return OrderResponseDto.from(order);
    }

    /**
     * 모든 주문을 페이징하여 조회합니다.
     *
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기
     * @param sort 정렬 방향 ("asc" 또는 "desc")
     * @return 주문 정보 응답 DTO 목록
     */
    public List<OrderResponseDto> getAllOrders(int page, int size, String sort) {
        List<Order> orders = orderRepository.findAll(page, size, sort);

        return orders.stream()
                .map(OrderResponseDto::from)
                .toList();
    }

    /**
     * 주문을 검색합니다.
     *
     * @param searchDto 주문 검색 조건 DTO
     * @return 주문 정보 응답 DTO 목록
     */
    public List<OrderResponseDto> searchOrders(OrderSearchDto searchDto) {
        try {
            List<Order> orders = orderRepository.search(
                    searchDto.getSearchTerm(),
                    searchDto.getStatus(),
                    searchDto.getStartDate(),
                    searchDto.getEndDate(),
                    searchDto.getCompanyId(),
                    searchDto.getHubId(),
                    searchDto.getProductId(),
                    searchDto.getPage(),
                    searchDto.getSize(),
                    searchDto.getSort()
            );

            // 검색 결과 총 개수 조회
            long totalCount = orderRepository.countSearchResults(
                    searchDto.getSearchTerm(),
                    searchDto.getStatus(),
                    searchDto.getStartDate(),
                    searchDto.getEndDate(),
                    searchDto.getCompanyId(),
                    searchDto.getHubId(),
                    searchDto.getProductId()
            );

            return orders.stream()
                    .map(OrderResponseDto::from)
                    .toList();

        } catch (Exception e) {
            log.error("주문 검색 중 오류 발생: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 주문을 업데이트합니다.
     *
     * @param orderId   업데이트할 주문 ID
     * @param updateDto 주문 업데이트 DTO
     * @return 업데이트된 주문 정보 응답 DTO
     * @throws CustomNotFoundException 주문이 존재하지 않는 경우
     * @throws CustomConflictException 이미 배송 중인 경우 또는 재고 부족 등의 문제가 있는 경우
     */
    @Transactional
    public OrderResponseDto updateOrder(UUID orderId, OrderUpdateDto updateDto) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomNotFoundException("주문을 찾을 수 없습니다. ID: " + orderId));

        // 주문 상태가 WAITING이 아니면 수정 불가
        if (order.getStatus() != OrderStatus.WAITING) {
            throw new CustomConflictException("배송이 이미 진행 중이므로 주문을 수정할 수 없습니다.");
        }

        // 배송 정보 확인 (배송이 이미 시작되었는지)
        if (order.getDeliverId() != null) {
            try {
                DeliveryDto deliveryInfo = deliveryClient.getDelivery(order.getDeliverId()).getData();

                // 배송 상태가 대기 중이 아니면 수정 불가
                if (!deliveryInfo.getStatus().equals("WAITING_AT_HUB")) {
                    throw new CustomConflictException("배송이 이미 진행 중이므로 주문을 수정할 수 없습니다.");
                }
            } catch (Exception e) {
                if (!(e instanceof CustomConflictException)) {
                    throw new CustomConflictException("배송 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
                }
                throw e;
            }
        }

        // 주문 정보 업데이트
        if (updateDto.getDeliverId() != null && updateDto.getHubId() != null &&
                updateDto.getCompanyId() != null) {
            orderDomainService.updateOrderInfo(
                    order,
                    updateDto.getDeliverId(),
                    updateDto.getHubId(),
                    updateDto.getCompanyId(),
                    updateDto.getOrderRequest(),
                    updateDto.getDeliveryDeadline()
            );
        }

        // 주문 상태 업데이트
        if (updateDto.getStatus() != null) {
            orderDomainService.updateOrderStatus(order, updateDto.getStatus());
        }

        // 주문 상품 업데이트 (재고 조정 포함)
        if (updateDto.getProducts() != null && !updateDto.getProducts().isEmpty()) {
            // 기존 주문 상품의 재고 복원
            for (OrderProduct orderProduct : order.getOrderProducts()) {
                try {
                    ProductDto product = companyClient.getProductById(orderProduct.getProductId()).getData();

                    ProductRequestDto restoreRequest = ProductRequestDto.builder()
                            .companyId(product.getCompanyId())
                            .name(product.getName())
                            .description(product.getDescription())
                            .price(product.getPrice())
                            .quantity(product.getQuantity() + orderProduct.getTotalQuantity())
                            .build();

                    companyClient.updateProduct(orderProduct.getProductId(), restoreRequest);
                } catch (Exception e) {
                    throw new CustomConflictException("상품 재고 복원 중 오류가 발생했습니다: " + e.getMessage());
                }
            }

            // 새로운 주문 상품 목록 생성
            List<OrderProduct> newProducts = new ArrayList<>();

            // 새로운 주문 상품에 대한 재고 확인 및 차감
            for (OrderUpdateDto.OrderProductDto productDto : updateDto.getProducts()) {
                try {
                    ProductDto product = companyClient.getProductById(productDto.getProductId()).getData();

                    // 재고 확인
                    if (product.getQuantity() < productDto.getQuantity()) {
                        throw new CustomConflictException("상품 재고가 부족합니다. 상품: " + product.getName() +
                                ", 현재 재고: " + product.getQuantity() + ", 요청 수량: " + productDto.getQuantity());
                    }

                    // 재고 감소
                    ProductRequestDto updateRequest = ProductRequestDto.builder()
                            .companyId(product.getCompanyId())
                            .name(product.getName())
                            .description(product.getDescription())
                            .price(product.getPrice())
                            .quantity(product.getQuantity() - productDto.getQuantity())
                            .build();

                    companyClient.updateProduct(productDto.getProductId(), updateRequest);

                    // 새 주문 상품 생성
                    OrderProduct orderProduct = orderDomainService.createOrderProduct(
                            productDto.getProductId(),
                            productDto.getQuantity(),
                            productDto.getTotalPrice() / productDto.getQuantity() // 단가 계산
                    );
                    newProducts.add(orderProduct);
                } catch (Exception e) {
                    if (!(e instanceof CustomConflictException)) {
                        throw new CustomConflictException("상품 정보 처리 중 오류가 발생했습니다: " + e.getMessage());
                    }
                    throw e;
                }
            }

            orderDomainService.updateOrderProducts(order, newProducts);
        }

        Order savedOrder = orderRepository.save(order);

        return OrderResponseDto.from(savedOrder);
    }

    /**
     * 주문을 삭제합니다. (논리적 삭제)
     *
     * @param orderId 삭제할 주문 ID
     * @throws CustomNotFoundException 주문이 존재하지 않는 경우
     * @throws CustomConflictException 이미 배송 중인 경우
     */
    @Transactional
    public void deleteOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomNotFoundException("주문을 찾을 수 없습니다. ID: " + orderId));

        // 주문 상태가 WAITING이 아니면 삭제 불가
        if (order.getStatus() != OrderStatus.WAITING) {
            throw new CustomConflictException("배송이 이미 진행 중이므로 주문을 삭제할 수 없습니다.");
        }

        // 배송 정보 확인 (배송이 이미 시작되었는지)
        if (order.getDeliverId() != null) {
            try {
                DeliveryDto deliveryInfo = deliveryClient.getDelivery(order.getDeliverId()).getData();

                // 배송 상태가 대기 중이 아니면 삭제 불가
                if (!deliveryInfo.getStatus().equals("WAITING_AT_HUB")) {
                    throw new CustomConflictException("배송이 이미 진행 중이므로 주문을 삭제할 수 없습니다.");
                }

                // 배송 삭제 요청
                deliveryClient.deleteDelivery(order.getDeliverId());
            } catch (Exception e) {
                if (!(e instanceof CustomConflictException)) {
                    throw new CustomConflictException("배송 정보 처리 중 오류가 발생했습니다: " + e.getMessage());
                }
                throw e;
            }
        }

        // 주문 상품의 재고 복원
        for (OrderProduct orderProduct : order.getOrderProducts()) {
            try {
                ProductDto product = companyClient.getProductById(orderProduct.getProductId()).getData();

                // 재고 복원
                ProductRequestDto updateRequest = ProductRequestDto.builder()
                        .companyId(product.getCompanyId())
                        .name(product.getName())
                        .description(product.getDescription())
                        .price(product.getPrice())
                        .quantity(product.getQuantity() + orderProduct.getTotalQuantity())
                        .build();

                companyClient.updateProduct(orderProduct.getProductId(), updateRequest);
            } catch (Exception e) {
                throw new CustomConflictException("상품 재고 복원 중 오류가 발생했습니다: " + e.getMessage());
            }
        }

        orderRepository.delete(order);
        log.info("주문 삭제 완료: orderId={}", orderId);
    }

    /**
     * 주문 상태만 업데이트합니다. (Kafka Consumer에서 사용)
     *
     * @param orderId   업데이트할 주문 ID
     * @param newStatus 새로운 주문 상태
     * @throws CustomNotFoundException 주문이 존재하지 않는 경우
     */
    @Transactional
    public void updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomNotFoundException("주문을 찾을 수 없습니다. ID: " + orderId));

        order.updateStatus(newStatus);
        orderRepository.save(order);

        log.info("주문 상태 업데이트 완료: orderId={}, newStatus={}", orderId, newStatus);
    }
}