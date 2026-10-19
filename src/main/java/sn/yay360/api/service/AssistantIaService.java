package sn.yay360.api.service;

import sn.yay360.api.dto.AssistantChatRequest;
import sn.yay360.api.dto.AssistantChatResponse;

public interface AssistantIaService {

    AssistantChatResponse repondre(String email, AssistantChatRequest request);
}
