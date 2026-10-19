package sn.yay360.api.service.impl;

import sn.yay360.api.dto.AssistantTranscriptionResponse;
import sn.yay360.api.helper.AssistantAudioHelper;
import sn.yay360.api.service.AssistantAudioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AssistantAudioServiceImpl implements AssistantAudioService {

    private final AssistantAudioHelper assistantAudioHelper;

    @Override
    public AssistantTranscriptionResponse transcrire(
            String email, MultipartFile audio, boolean consentementTransmission) {
        return assistantAudioHelper.transcrire(email, audio, consentementTransmission);
    }

    @Override
    public byte[] synthetiser(String email, String text, Boolean consentementTransmission) {
        return assistantAudioHelper.synthetiser(email, text, consentementTransmission);
    }
}
