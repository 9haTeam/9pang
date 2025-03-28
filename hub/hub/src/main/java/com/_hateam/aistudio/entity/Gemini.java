package com._hateam.aistudio.entity;

import com._hateam.common.entity.Timestamped;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 빌더를 통한 생성만 허용
@Builder
@Table(name = "p_gemini")
public class Gemini extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "gemini_id", columnDefinition = "uuid")
    private UUID id;

    @Column(length = 255)
    private String question; // 요청한 질문(프롬프트)

    @Column(length = 500)
    private String answer;

    @Column(nullable = false)
    @Builder.Default
    private boolean isDeleted = false;
}
