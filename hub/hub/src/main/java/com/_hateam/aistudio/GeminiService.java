package com._hateam.aistudio;


import com._hateam.aistudio.dto.CreatedGeminiResponseDto;
import com._hateam.aistudio.dto.GeminiRequestDto;
import com._hateam.aistudio.dto.GeminiResponseDto;
import com._hateam.aistudio.entity.Gemini;
import com._hateam.aistudio.feign.GeminiClient;
import com._hateam.aistudio.repository.GeminiRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j(topic = "Gemini Service")
@RequiredArgsConstructor
@Service
public class GeminiService {

    private final GeminiClient geminiClient;
    private final GeminiRepository geminiRepository;

    @Transactional
    public CreatedGeminiResponseDto generateContent(GeminiRequestDto requestDto) {
        String originalPrompt = requestDto.getContents()
                .get(0)
                .getParts()
                .get(0)
                .getText();
        String finalPrompt = originalPrompt + SendToAiMessage.ADDITIONAL_MESSAGE.getSendToAiMessage();

        GeminiRequestDto modifiedRequest = new GeminiRequestDto(
                List.of(new GeminiRequestDto.Content(
                        List.of(new GeminiRequestDto.Part(finalPrompt))
                ))
        );

        GeminiResponseDto response = geminiClient.generateContent(modifiedRequest);

        // 응답 유효성 검사
        if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
            throw new IllegalStateException("Failed to generate content from Gemini");
        }

        // 응답 추출
        String finalResponse = response.getCandidates()
                .get(0)
                .getContent()
                .getParts()
                .get(0)
                .getText();

        // 저장
        Gemini gemini = saveGeneratedContent(Gemini.builder()
                .question(finalPrompt)
                .answer(finalResponse)
                .build());

        // 응답 반환
        return CreatedGeminiResponseDto.from(gemini);
    }

//    @Transactional
//    public void softDeleteGemini(UUID geminiId, UserDetails userDetails) {
//        // 삭제되지 않은 행만 조회하도록 처리
//        Gemini gemini = geminiRepository.findByIdAndIsDeletedFalse(geminiId)
//                .orElseThrow(() -> new EntityNotFoundException("Gemini record not found with id: " + geminiId));
//
//        // isDeleted 플래그를 true로 설정하여 숨김 처리
//        gemini.setDeleted(true);
//
//        // Timestamped의 delete 메소드가 있다면 아래와 같이 호출하여 deletedAt, deletedBy를 기록
//        gemini.delete(userDetails.getUsername());
//    }

    private Gemini saveGeneratedContent(Gemini gemini) {
        return geminiRepository.save(gemini);
    }
}
