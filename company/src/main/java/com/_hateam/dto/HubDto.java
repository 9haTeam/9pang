package com._hateam.dto;

import com._hateam.entity.Hub;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HubDto implements Serializable { // Serializable 인터페이스 구현

    @NotNull(message = "허브 ID는 필수입니다.")
    private UUID id;

    @NotNull(message = "허브 명은 필수입니다.")
    @Pattern(regexp = "^[a-zA-Z0-9가-힣\\s]{1,20}$", message = "허브 명은 특수문자를 제외한 20자 이하여야 합니다.")
    private String name;

    @NotNull(message = "주소는 필수입니다.")
    @Size(max = 255, message = "주소는 255자 이하여야 합니다.")
    private String address;

    @NotNull(message = "위도는 필수입니다.")
    @Size(max = 20, message = "위도는 20자 이하여야 합니다.")
    private String latitude;

    @NotNull(message = "경도는 필수입니다.")
    @Size(max = 20, message = "경도는 20자 이하여야 합니다.")
    private String longitude;

    // Hub 엔티티를 HubDto로 변환하는 정적 팩토리 메소드
    public static HubDto hubToHubDto(Hub hub) {
        return HubDto.builder()
                .id(hub.getId())
                .name(hub.getName())
                .address(hub.getAddress())
                .latitude(hub.getLatitude())
                .longitude(hub.getLongitude())
                .build();
    }

    public static Hub hubToHubDto(HubDto hubDto) {
        return Hub.builder()
                .id(hubDto.getId())
                .name(hubDto.getName())
                .address(hubDto.getAddress())
                .latitude(hubDto.getLatitude())
                .longitude(hubDto.getLongitude())
                .build();
    }
}