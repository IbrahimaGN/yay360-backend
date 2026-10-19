package sn.yay360.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AssistantTranscriptionResponse {
    private String transcription;
    private String avertissement;
}
