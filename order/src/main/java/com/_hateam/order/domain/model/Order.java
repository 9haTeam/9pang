package com._hateam.order.domain.model;

import com._hateam.common.entity.Timestamped;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "p_order")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends Timestamped {

    @Id
    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "deliver_id")
    private UUID deliverId;

    @Column(name = "hub_id", nullable = false)
    private UUID hubId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "order_request")
    private String orderRequest;

    @Column(name = "delivery_deadline", nullable = false)
    private LocalDateTime deliveryDeadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderProduct> orderProducts = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.orderId == null) {
            this.orderId = UUID.randomUUID();
        }
        if (this.status == null) {
            this.status = OrderStatus.WAITING;
        }
    }

    // 상품 추가 메서드
    public void addOrderProduct(OrderProduct orderProduct) {
        this.orderProducts.add(orderProduct);
        orderProduct.setOrder(this);
    }

    // 상태 변경 메서드
    public void updateStatus(OrderStatus newStatus) {
        this.status = newStatus;
    }

    // 주문 정보 수정 메서드
    public void updateOrderInfo(UUID deliverId, UUID hubId, UUID companyId,
                                String orderRequest, LocalDateTime deliveryDeadline) {
        this.deliverId = deliverId;
        this.hubId = hubId;
        this.companyId = companyId;
        this.orderRequest = orderRequest;
        this.deliveryDeadline = deliveryDeadline;
    }

    public void updateDeliveryId(UUID deliveryId) {
        this.deliverId = deliveryId;
    }

    // 총 가격 계산 및 업데이트
    public void calculateTotalPrice() {
        if (this.orderProducts == null || this.orderProducts.isEmpty()) {
            this.totalPrice = 0; // 상품이 없는 경우 0으로 설정
        } else {
            this.totalPrice = this.orderProducts.stream()
                    .mapToInt(OrderProduct::getTotalPrice)
                    .sum();
        }
    }

    public void setTotalPrice(Integer totalPrice) {
        this.totalPrice = totalPrice;
    }
}