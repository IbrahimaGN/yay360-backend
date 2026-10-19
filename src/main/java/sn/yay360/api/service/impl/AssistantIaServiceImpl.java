package sn.yay360.api.service.impl;

import sn.yay360.api.dto.AssistantChatRequest;
import sn.yay360.api.dto.AssistantChatResponse;
import sn.yay360.api.helper.AssistantIaHelper;
import sn.yay360.api.service.AssistantIaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssistantIaServiceImpl implements AssistantIaService {

    private final AssistantIaHelper assistantIaHelper;

    @Override
    public AssistantChatResponse repondre(String email, AssistantChatRequest request) {
        return assistantIaHelper.repondre(email, request);
    }
}
