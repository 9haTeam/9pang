package com._hateam.aistudio;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SendToAiMessage {
    ADDITIONAL_MESSAGE(", 답변을 최대한 간결하게 50자 이하로.");
    private final String SendToAiMessage;

}
