package com._hateam.delivery.dto.response;

import com._hateam.delivery.entity.Delivery;
import com._hateam.delivery.entity.DeliveryStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
public class DeliveryResponseDto {

    private UUID id;
    private UUID orderId;
    private DeliveryStatus status;
    private UUID startHubId;
    private UUID endHubId;
    private String receiverAddress;
    private String receiverName;
    private String receiverSlackId;
    private UUID deliverId;
    private String deliverSlackId;

    @Builder
    public DeliveryResponseDto(UUID id, UUID orderId,
                               DeliveryStatus status, UUID startHubId, UUID endHubId,
                               String receiverAddress, String receiverName, String receiverSlackId,
                               UUID deliverId, String deliverSlackId) {
        this.id = id;
        this.orderId = orderId;
        this.status = status;
        this.startHubId = startHubId;
        this.endHubId = endHubId;
        this.receiverAddress = receiverAddress;
        this.receiverName = receiverName;
        this.receiverSlackId = receiverSlackId;
        this.deliverId = deliverId;
        this.deliverSlackId = deliverSlackId;
    }


    /**
     * todo: mapper로 분리?
     */
    public static DeliveryResponseDto from(Delivery delivery) {

        return DeliveryResponseDto.builder()
                .id(delivery.getId())
                .orderId(delivery.getOrderId())
                .status(delivery.getStatus())
                .startHubId(delivery.getStartHubId())
                .endHubId(delivery.getEndHubId())
                .receiverAddress(delivery.getReceiverAddress())
                .receiverName(delivery.getReceiverName())
                .receiverSlackId(delivery.getReceiverSlackId())
                .deliverId(delivery.getDeliverId())
                .deliverSlackId(delivery.getDeliverSlackId())
                .build();
    }
}
