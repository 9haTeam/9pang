package com._hateam.gateway.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserRole {
    ADMIN("ADMIN"), //마스터 관리자
    HUB("HUB"),//허브 담당자
    DELIVERY("DELIVERY"),//배송 담당자
    COMPANY("COMPANY"); //업체 관리자

    private final String role;
}
