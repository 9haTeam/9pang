package com._hateam.delivery.dto.response;

import com._hateam.delivery.entity.OrderStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@Setter // todo: 임시객체 생성을 위해 사용중, 나중에 삭제할것 요청값으로 받을 수도 있다 보류처리
public class OrderClientResponseDto {
    private UUID orderId;
    private UUID hubId;
    private UUID companyId;
    private String orderRequest;
    private OrderStatus orderStatus;


}
