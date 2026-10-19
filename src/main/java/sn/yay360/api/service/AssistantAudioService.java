package sn.yay360.api.service;

import sn.yay360.api.dto.AssistantTranscriptionResponse;
import org.springframework.web.multipart.MultipartFile;

public interface AssistantAudioService {

    AssistantTranscriptionResponse transcrire(String email, MultipartFile audio, boolean consentementTransmission);

    byte[] synthetiser(String email, String text, Boolean consentementTransmission);
}
