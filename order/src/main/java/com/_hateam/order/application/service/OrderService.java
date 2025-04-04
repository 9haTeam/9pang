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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;

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

        // 주문 상품 재고 확인을 먼저 수행
        Map<UUID, ProductDto> productMap = new HashMap<>();
        for (OrderRequestDto.OrderProductDto productDto : requestDto.getProducts()) {
            try {
                ProductDto product = companyClient.getProductById(productDto.getProductId()).getData();
                productMap.put(productDto.getProductId(), product);

                // 재고 확인
                if (product.getQuantity() < productDto.getQuantity()) {
                    throw new CustomConflictException("상품 재고가 부족합니다. 상품: " + product.getName() +
                            ", 현재 재고: " + product.getQuantity() + ", 요청 수량: " + productDto.getQuantity());
                }

            } catch (Exception e) {
                if (!(e instanceof CustomConflictException)) {
                    throw new CustomConflictException("상품 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
                }
                throw e;
            }
        }

        // 주문 상품 생성 및 재고 감소
        List<OrderProduct> successfullyAddedProducts = new ArrayList<>();
        try {
            for (OrderRequestDto.OrderProductDto productDto : requestDto.getProducts()) {
                ProductDto product = productMap.get(productDto.getProductId());

                // 단가 계산 시 0으로 나누기 방지
                int quantity = productDto.getQuantity();
                int unitPrice = quantity > 0 ? productDto.getTotalPrice() / quantity : 0;

                // 주문 상품 생성
                OrderProduct orderProduct = orderDomainService.createOrderProduct(
                        productDto.getProductId(),
                        quantity,
                        unitPrice
                );
                order.addOrderProduct(orderProduct);
                successfullyAddedProducts.add(orderProduct);

                // 재고 감소
                try {
                    ProductRequestDto updateRequest = ProductRequestDto.builder()
                            .companyId(product.getCompanyId())
                            .name(product.getName())
                            .description(product.getDescription())
                            .price(product.getPrice())
                            .quantity(product.getQuantity() - quantity)
                            .build();

                    companyClient.updateProduct(productDto.getProductId(), updateRequest);
                } catch (Exception e) {
                    // 롤백을 위해 예외를 던짐
                    throw new CustomConflictException("상품 재고 업데이트 중 오류가 발생했습니다: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            // 문제 발생 시 이미 처리된 주문 상품들의 재고 복원
            rollbackInventory(successfullyAddedProducts, productMap);
            throw e;
        }

        // 총 가격 계산
        order.calculateTotalPrice();

        Order savedOrder = orderRepository.save(order);

        // 주문 생성 이벤트 발행
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(savedOrder.getOrderId())
                .hubId(savedOrder.getHubId())
                .companyId(savedOrder.getCompanyId())
                .orderRequest(savedOrder.getOrderRequest())
                .deliveryDeadline(savedOrder.getDeliveryDeadline())
                .build();

        kafkaTemplate.send(KafkaTopics.ORDER_CREATED, event);

        // 슬랙 알림을 비동기로 전송
        sendOrderCreatedSlackNotificationAsync(savedOrder);

        return OrderResponseDto.from(savedOrder);
    }

    // 재고 롤백 메서드
    private void rollbackInventory(List<OrderProduct> products, Map<UUID, ProductDto> productMap) {
        for (OrderProduct orderProduct : products) {
            try {
                ProductDto product = productMap.get(orderProduct.getProductId());
                if (product != null) {
                    ProductRequestDto restoreRequest = ProductRequestDto.builder()
                            .companyId(product.getCompanyId())
                            .name(product.getName())
                            .description(product.getDescription())
                            .price(product.getPrice())
                            .quantity(product.getQuantity()) // 원래 수량으로 복원
                            .build();

                    companyClient.updateProduct(orderProduct.getProductId(), restoreRequest);
                    log.info("상품 ID: {}의 재고를 원상복구했습니다.", orderProduct.getProductId());
                }
            } catch (Exception e) {
                log.error("상품 ID: {}의 재고 롤백 중 오류 발생: {}", orderProduct.getProductId(), e.getMessage());
                // 롤백 중 오류가 발생해도 계속 진행
            }
        }
    }

    // 슬랙 알림을 위한 비동기 이벤트 발행 메서드
    @Async
    public void sendOrderCreatedSlackNotificationAsync(Order order) {
        // 배송 ID가 null인 경우 슬랙 알림을 보내지 않음
        if (order.getDeliverId() == null) {
            log.info("배송 ID가 없어 슬랙 알림을 건너뜁니다. 주문 ID: {}", order.getOrderId());
            return;
        }

        try {
            // 업체 정보 조회
            CompanyDto company = companyClient.getCompanyById(order.getCompanyId()).getData();
            if (company == null) {
                log.error("주문 ID: {}에 대한 업체 정보를 찾을 수 없습니다.", order.getOrderId());
                return;
            }

            // 주문 상품이 없는 경우 처리
            if (order.getOrderProducts() == null || order.getOrderProducts().isEmpty()) {
                log.error("주문 ID: {}에 대한 상품 정보가 없습니다.", order.getOrderId());
                return;
            }

            // 상품 정보 조회 - 첫 번째 상품만 예시로 표시
            OrderProduct firstProduct = order.getOrderProducts().get(0);
            ProductDto product;
            try {
                product = companyClient.getProductById(firstProduct.getProductId()).getData();
                if (product == null) {
                    log.error("상품 ID: {}에 대한 정보를 찾을 수 없습니다.", firstProduct.getProductId());
                    return;
                }
            } catch (Exception e) {
                log.error("상품 정보 조회 중 오류 발생: {}", e.getMessage());
                return;
            }

            // 배송 정보 조회
            DeliveryDto delivery;
            try {
                delivery = deliveryClient.getDelivery(order.getDeliverId()).getData();
                if (delivery == null) {
                    log.error("배송 ID: {}에 대한 정보를 찾을 수 없습니다.", order.getDeliverId());
                    return;
                }

                // delivery의 필수 필드 검증
                if (delivery.getStartHubId() == null) {
                    log.error("배송 정보에 출발 허브 ID가 없습니다. 배송 ID: {}", order.getDeliverId());
                    return;
                }

                if (delivery.getDelivererId() == null) {
                    log.error("배송 정보에 배송원 ID가 없습니다. 배송 ID: {}", order.getDeliverId());
                    return;
                }
            } catch (Exception e) {
                log.error("배송 정보 조회 중 오류 발생: {}", e.getMessage());
                return;
            }

            // 허브 정보 조회
            HubDto startHub;
            try {
                startHub = hubClient.getHubById(delivery.getStartHubId()).getData();
                if (startHub == null) {
                    log.error("허브 ID: {}에 대한 정보를 찾을 수 없습니다.", delivery.getStartHubId());
                    return;
                }
            } catch (Exception e) {
                log.error("허브 정보 조회 중 오류 발생: {}", e.getMessage());
                return;
            }

            // 허브 경로 정보 조회 (실제 구현으로 대체 필요)
            List<String> viaHubs = new ArrayList<>();
            try {
                // TODO: 실제 배송 경로 정보를 조회하여 허브 경로 목록 설정
                // 하드코딩된 값 대신 실제 경로 조회 로직 구현 필요
                viaHubs.add("중간 경유지");
            } catch (Exception e) {
                log.error("허브 경로 정보 조회 중 오류 발생: {}", e.getMessage());
                // 경로 정보는 필수가 아니므로 계속 진행
            }

            // 배송 담당자 정보 조회
            DeliverUserDto deliverer;
            try {
                deliverer = userClient.getDeliverUserById(delivery.getDelivererId()).getData();
                if (deliverer == null) {
                    log.error("배송원 ID: {}에 대한 정보를 찾을 수 없습니다.", delivery.getDelivererId());
                    return;
                }
            } catch (Exception e) {
                log.error("배송원 정보 조회 중 오류 발생: {}", e.getMessage());
                return;
            }

            // 슬랙 알림 이벤트 생성 및 발송
            OrderCreatedForSlackEvent event = OrderCreatedForSlackEvent.builder()
                    .orderId(order.getOrderId())
                    .orderNumber(order.getOrderId().toString().substring(0, 8)) // 간략화된 주문번호
                    .customerName(company.getCompanyName())
                    .customerEmail(company.getUserId() + "@delivery.com") // 예시 이메일 대신 사용자 ID 기반 생성
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
                if (deliveryInfo != null && !deliveryInfo.getStatus().equals("WAITING_AT_HUB")) {
                    throw new CustomConflictException("배송이 이미 진행 중이므로 주문을 수정할 수 없습니다.");
                }
            } catch (Exception e) {
                if (!(e instanceof CustomConflictException)) {
                    throw new CustomConflictException("배송 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
                }
                throw e;
            }
        }

        // 주문 정보 업데이트 - 개별 필드 업데이트 허용
        UUID deliverId = updateDto.getDeliverId() != null ? updateDto.getDeliverId() : order.getDeliverId();
        UUID hubId = updateDto.getHubId() != null ? updateDto.getHubId() : order.getHubId();
        UUID companyId = updateDto.getCompanyId() != null ? updateDto.getCompanyId() : order.getCompanyId();
        String orderRequest = updateDto.getOrderRequest() != null ? updateDto.getOrderRequest() : order.getOrderRequest();

        // 배송 마감일이 변경될 경우에만 업데이트
        if (updateDto.getDeliveryDeadline() != null) {
            orderDomainService.updateOrderInfo(
                    order,
                    deliverId,
                    hubId,
                    companyId,
                    orderRequest,
                    updateDto.getDeliveryDeadline()
            );
        } else {
            orderDomainService.updateOrderInfo(
                    order,
                    deliverId,
                    hubId,
                    companyId,
                    orderRequest,
                    order.getDeliveryDeadline()
            );
        }

        // 주문 상태 업데이트
        if (updateDto.getStatus() != null) {
            orderDomainService.updateOrderStatus(order, updateDto.getStatus());
        }

        // 주문 상품 업데이트 (재고 조정 포함)
        if (updateDto.getProducts() != null && !updateDto.getProducts().isEmpty()) {
            Map<UUID, ProductDto> productMap = new HashMap<>();
            List<OrderProduct> successfullyRestoredProducts = new ArrayList<>();

            try {
                // 1. 기존 주문 상품의 재고 복원
                for (OrderProduct orderProduct : order.getOrderProducts()) {
                    try {
                        ProductDto product = companyClient.getProductById(orderProduct.getProductId()).getData();
                        productMap.put(orderProduct.getProductId(), product);

                        ProductRequestDto restoreRequest = ProductRequestDto.builder()
                                .companyId(product.getCompanyId())
                                .name(product.getName())
                                .description(product.getDescription())
                                .price(product.getPrice())
                                .quantity(product.getQuantity() + orderProduct.getTotalQuantity())
                                .build();

                        companyClient.updateProduct(orderProduct.getProductId(), restoreRequest);
                        successfullyRestoredProducts.add(orderProduct);
                    } catch (Exception e) {
                        // 실패한 경우 롤백을 위해 예외를 던짐
                        throw new CustomConflictException("상품 재고 복원 중 오류가 발생했습니다: " + e.getMessage());
                    }
                }

                // 2. 새로운 주문 상품 목록 생성
                List<OrderProduct> newProducts = new ArrayList<>();
                List<OrderUpdateDto.OrderProductDto> successfullyCheckedProducts = new ArrayList<>();

                // 3. 새로운 주문 상품들의 재고 확인
                for (OrderUpdateDto.OrderProductDto productDto : updateDto.getProducts()) {
                    try {
                        ProductDto product = companyClient.getProductById(productDto.getProductId()).getData();
                        if (!productMap.containsKey(productDto.getProductId())) {
                            productMap.put(productDto.getProductId(), product);
                        }

                        // 재고 확인
                        if (product.getQuantity() < productDto.getQuantity()) {
                            throw new CustomConflictException("상품 재고가 부족합니다. 상품: " + product.getName() +
                                    ", 현재 재고: " + product.getQuantity() + ", 요청 수량: " + productDto.getQuantity());
                        }

                        successfullyCheckedProducts.add(productDto);
                    } catch (Exception e) {
                        if (!(e instanceof CustomConflictException)) {
                            throw new CustomConflictException("상품 정보 처리 중 오류가 발생했습니다: " + e.getMessage());
                        }
                        throw e;
                    }
                }

                // 4. 새로운 주문 상품에 대한 재고 차감 및 주문 상품 생성
                for (OrderUpdateDto.OrderProductDto productDto : successfullyCheckedProducts) {
                    try {
                        ProductDto product = productMap.get(productDto.getProductId());

                        // 재고 감소
                        ProductRequestDto updateRequest = ProductRequestDto.builder()
                                .companyId(product.getCompanyId())
                                .name(product.getName())
                                .description(product.getDescription())
                                .price(product.getPrice())
                                .quantity(product.getQuantity() - productDto.getQuantity())
                                .build();

                        companyClient.updateProduct(productDto.getProductId(), updateRequest);

                        // 단가 계산 시 0으로 나누기 방지
                        int quantity = productDto.getQuantity();
                        int unitPrice = quantity > 0 ? productDto.getTotalPrice() / quantity : 0;

                        // 새 주문 상품 생성
                        OrderProduct orderProduct = orderDomainService.createOrderProduct(
                                productDto.getProductId(),
                                quantity,
                                unitPrice
                        );
                        newProducts.add(orderProduct);
                    } catch (Exception e) {
                        throw new CustomConflictException("상품 정보 처리 중 오류가 발생했습니다: " + e.getMessage());
                    }
                }

                // 5. 주문 상품 업데이트
                orderDomainService.updateOrderProducts(order, newProducts);

            } catch (Exception e) {
                // 오류 발생 시 재고 롤백: 복원된 재고는 다시 차감, 차감된 재고는 다시 복원
                rollbackUpdateInventory(successfullyRestoredProducts, productMap);
                throw e;
            }
        }

        Order savedOrder = orderRepository.save(order);

        return OrderResponseDto.from(savedOrder);
    }

    // 주문 업데이트 시 재고 롤백 메서드
    private void rollbackUpdateInventory(List<OrderProduct> restoredProducts, Map<UUID, ProductDto> productMap) {
        for (OrderProduct orderProduct : restoredProducts) {
            try {
                ProductDto originalProduct = productMap.get(orderProduct.getProductId());
                if (originalProduct != null) {
                    // 다시 원래 상태로 되돌리기 (재고 차감)
                    ProductRequestDto rollbackRequest = ProductRequestDto.builder()
                            .companyId(originalProduct.getCompanyId())
                            .name(originalProduct.getName())
                            .description(originalProduct.getDescription())
                            .price(originalProduct.getPrice())
                            .quantity(originalProduct.getQuantity() - orderProduct.getTotalQuantity())
                            .build();

                    companyClient.updateProduct(orderProduct.getProductId(), rollbackRequest);
                    log.info("상품 ID: {}의 재고를 롤백했습니다.", orderProduct.getProductId());
                }
            } catch (Exception e) {
                log.error("상품 ID: {}의 재고 롤백 중 오류 발생: {}", orderProduct.getProductId(), e.getMessage());
                // 롤백 중 오류가 발생해도 계속 진행
            }
        }
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
                if (deliveryInfo != null && !deliveryInfo.getStatus().equals("WAITING_AT_HUB")) {
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

        // 상품 정보 조회 및 저장
        Map<UUID, ProductDto> productMap = new HashMap<>();
        for (OrderProduct orderProduct : order.getOrderProducts()) {
            try {
                ProductDto product = companyClient.getProductById(orderProduct.getProductId()).getData();
                productMap.put(orderProduct.getProductId(), product);
            } catch (Exception e) {
                throw new CustomConflictException("상품 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
            }
        }

        // 주문 상품의 재고 복원
        List<OrderProduct> successfullyRestoredProducts = new ArrayList<>();
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

    // 삭제 시 재고 롤백 메서드
    private void rollbackDeleteInventory(List<OrderProduct> restoredProducts, Map<UUID, ProductDto> productMap) {
        for (OrderProduct orderProduct : restoredProducts) {
            try {
                ProductDto product = productMap.get(orderProduct.getProductId());
                if (product != null) {
                    // 다시 원래 상태로 되돌리기 (재고 차감)
                    ProductRequestDto rollbackRequest = ProductRequestDto.builder()
                            .companyId(product.getCompanyId())
                            .name(product.getName())
                            .description(product.getDescription())
                            .price(product.getPrice())
                            .quantity(product.getQuantity()) // 원래 수량으로 복원
                            .build();

                    companyClient.updateProduct(orderProduct.getProductId(), rollbackRequest);
                    log.info("삭제 취소: 상품 ID: {}의 재고를 원상복구했습니다.", orderProduct.getProductId());
                }
            } catch (Exception e) {
                log.error("삭제 취소: 상품 ID: {}의 재고 롤백 중 오류 발생: {}", orderProduct.getProductId(), e.getMessage());
                // 롤백 중 오류가 발생해도 계속 진행
            }
        }
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

    /**
     * 배송 ID를 주문 정보에 업데이트합니다. (Kafka Consumer에서 사용)
     *
     * @param orderId    주문 ID
     * @param deliveryId 배송 ID
     * @throws CustomNotFoundException 주문이 존재하지 않는 경우
     */
    @Transactional
    public void updateDeliveryId(UUID orderId, UUID deliveryId) {
        log.info("배송 ID 업데이트 시작: 주문 ID={}, 배송 ID={}", orderId, deliveryId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("주문을 찾을 수 없음: ID={}", orderId);
                    return new CustomNotFoundException("주문을 찾을 수 없습니다. ID: " + orderId);
                });

        // 현재 배송 ID 기록
        UUID oldDeliverId = order.getDeliverId();
        log.info("기존 배송 ID: {}", oldDeliverId);

        // 배송 ID 직접 설정 (추가)
        order.updateDeliveryId(deliveryId);

        // orderDomainService를 통한 업데이트도 유지
        orderDomainService.updateOrderInfo(
                order,
                deliveryId,
                order.getHubId(),
                order.getCompanyId(),
                order.getOrderRequest(),
                order.getDeliveryDeadline()
        );

        Order savedOrder = orderRepository.save(order);
        log.info("배송 ID 업데이트 완료: 주문 ID={}, 이전 배송 ID={}, 새 배송 ID={}",
                orderId, oldDeliverId, savedOrder.getDeliverId());
    }
}