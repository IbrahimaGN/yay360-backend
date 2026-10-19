package sn.yay360.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AssistantChatResponse {

    private String reponse;
    private String canal;
    private String avertissement;
    private String texteALire;
}
