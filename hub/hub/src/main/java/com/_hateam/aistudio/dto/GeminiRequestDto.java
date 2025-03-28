package com._hateam.aistudio.dto;

import com._hateam.entity.Hub;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeminiRequestDto {
    private List<Content> contents;

//    /**
//     * 주어진 지역(region)에 대한 프롬프트 메시지를 설정합니다.
//     * 프롬프트 형식:
//     * "{region} 의 지도 정보가 필요해. 답변은 정확히 'name:ex1, address:ex2, latitude:ex3, longitude:ex4'의 형식으로만 답변해."
//     *
//     * @param region 해당 지역명
//     */
//    public void setPrompt(String region) {
//        String promptText = region + " 위치의 위도와 경도 정보가 필요해. 답변은 정확히 'latitude:ex1, longitude:ex2'의 형식으로만 답변해.";
//        Part part = Part.builder().text(promptText).build();
//        Content content = Content.builder().parts(Collections.singletonList(part)).build();
//        this.contents = Collections.singletonList(content);
//    }
//    ->>>>>>>>
//    for (String region : regions) {
//        GeminiRequestDto requestDto = new GeminiRequestDto();
//        // 프롬프트 설정: 예) "Seoul 의 지도 정보가 필요해.
//        // 답변은 정확히 'name:ex1, address:ex2, latitude:ex3, longitude:ex4'의 형식으로만 답변해.
//        requestDto.setPrompt(region);
//
//        // Gemini API 호출
//        GeminiResponseDto response = geminiClient.generateContent(requestDto);
//        if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
//            log.warn("No response received for region: {}", region);
//            continue;
//        }
//
//        // 응답 텍스트 추출
//        // 가정: 응답 형식은 "name:ex1, address:ex2, latitude:ex3, longitude:ex4"
//        String responseText = response.getCandidates()
//                .get(0)
//                .getContent()
//                .getParts()
//                .get(0)
//                .getText();
//        log.info("Response for {}: {}", region, responseText);
//        String[] parts = responseText.split(",");
//        if (parts.length < 4) {
//            log.warn("Invalid response format for region: {}. Response: {}", region, responseText);
//            continue;
//        }
//        try {
//            String name = parts[0].split(":")[1].trim();
//            String address = parts[1].split(":")[1].trim();
//            String latitude = parts[2].split(":")[1].trim();
//            String longitude = parts[3].split(":")[1].trim();
//
//            // Hub 엔티티 생성 및 저장 (캐싱 적용됨)
//            Hub hub = saveHub(name, address, latitude, longitude);
//            log.info("Hub created for region {}: {}", region, hub);
//        } catch (Exception e) {
//            log.warn("Error parsing response for region {}. Response: {}", region, responseText, e);
//        }
//        try {
//            // 각 API 호출 후 20초 대기
//            Thread.sleep(20000);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
//    }
//        log.info("Hub initializer finished. Now calculating hub graph distances...");

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Content {
        private List<Part> parts;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Part {
        private String text;
    }
}
