package sn.yay360.api.helper;

import sn.yay360.api.config.AssistantIaProperties;
import sn.yay360.api.dto.AssistantTranscriptionResponse;
import sn.yay360.api.exception.AssistantIndisponibleException;
import sn.yay360.api.exception.OperationNonAutoriseeException;
import sn.yay360.api.repo.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AssistantAudioHelper {

    private static final String AVERTISSEMENT =
            "Cet assistant fournit uniquement des informations générales et ne remplace pas un professionnel de santé.";
    private static final Set<String> FORMATS_AUDIO = Set.of(
            "audio/mpeg", "audio/mp3", "audio/mpga", "audio/mp4", "audio/x-m4a", "audio/m4a",
            "audio/wav", "audio/x-wav", "audio/webm", "audio/ogg", "audio/flac", "audio/aac"
    );

    private final RestClient assistantIaRestClient;
    private final AssistantIaProperties properties;
    private final UtilisateurRepository utilisateurRepository;

    @PreAuthorize("hasAnyAuthority('FEMME_ENCEINTE', 'CONJOINT')")
    public AssistantTranscriptionResponse transcrire(
            String email, MultipartFile audio, boolean consentementTransmission) {
        verifierUtilisateurEtConsentement(email, consentementTransmission);
        verifierConfiguration(properties.getTranscriptionEndpoint(), properties.getTranscriptionModel());
        if (audio == null || audio.isEmpty()) {
            throw new IllegalArgumentException("Le fichier audio est vide ou absent");
        }
        if (audio.getSize() > properties.getMaxAudioBytes()) {
            throw new IllegalArgumentException("Le fichier audio dépasse la taille maximale autorisée");
        }
        String mediaType = audio.getContentType();
        String mediaTypeBase = mediaType == null ? "" : mediaType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (!FORMATS_AUDIO.contains(mediaTypeBase)) {
            throw new IllegalArgumentException("Format audio non pris en charge");
        }

        ByteArrayResource fichier = new ByteArrayResource(readAudio(audio)) {
            @Override
            public String getFilename() {
                String original = audio.getOriginalFilename();
                if (original == null || original.isBlank()) {
                    return "enregistrement-audio";
                }
                return original.replaceAll("[^A-Za-z0-9._-]", "_");
            }
        };
        LinkedMultiValueMap<String, Object> formulaire = new LinkedMultiValueMap<>();
        formulaire.add("file", fichier);
        formulaire.add("model", properties.getTranscriptionModel());
        formulaire.add("response_format", "json");
        formulaire.add("language", properties.getTranscriptionLanguage());

        TranscriptionProviderResponse reponse;
        try {
            reponse = assistantIaRestClient.post()
                    .uri(properties.getTranscriptionEndpoint())
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(formulaire)
                    .retrieve()
                    .body(TranscriptionProviderResponse.class);
        } catch (RestClientException exception) {
            throw new AssistantIndisponibleException(
                    "Le service de transcription est temporairement indisponible.", exception);
        }
        if (reponse == null || reponse.text() == null || reponse.text().isBlank()) {
            throw new AssistantIndisponibleException("Le service de transcription n'a pas retourné de texte.");
        }
        return new AssistantTranscriptionResponse(reponse.text().trim(), AVERTISSEMENT);
    }

    @PreAuthorize("hasAnyAuthority('FEMME_ENCEINTE', 'CONJOINT')")
    public byte[] synthetiser(String email, String text, Boolean consentementTransmission) {
        verifierUtilisateurEtConsentement(email, Boolean.TRUE.equals(consentementTransmission));
        verifierConfiguration(properties.getSpeechEndpoint(), properties.getSpeechModel());
        if (text == null || text.isBlank() || text.length() > 4000) {
            throw new IllegalArgumentException("Le texte à vocaliser doit contenir entre 1 et 4000 caractères");
        }

        byte[] audio;
        try {
            audio = assistantIaRestClient.post()
                    .uri(properties.getSpeechEndpoint())
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.parseMediaType("audio/mpeg"))
                    .body(Map.of(
                            "model", properties.getSpeechModel(),
                            "input", text,
                            "voice", properties.getSpeechVoice(),
                            "response_format", "mp3"
                    ))
                    .retrieve()
                    .body(byte[].class);
        } catch (RestClientException exception) {
            throw new AssistantIndisponibleException(
                    "Le service de synthèse vocale est temporairement indisponible.", exception);
        }
        if (audio == null || audio.length == 0) {
            throw new AssistantIndisponibleException("Le service de synthèse vocale n'a pas retourné d'audio.");
        }
        return audio;
    }

    private void verifierUtilisateurEtConsentement(String email, boolean consentementTransmission) {
        utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new OperationNonAutoriseeException("Compte utilisateur introuvable"));
        if (!consentementTransmission) {
            throw new OperationNonAutoriseeException(
                    "Un consentement explicite est requis avant la transmission au fournisseur IA");
        }
    }

    private void verifierConfiguration(String endpoint, String model) {
        if (!properties.isEnabled() || properties.getApiKey() == null || properties.getApiKey().isBlank()
                || endpoint == null || endpoint.isBlank() || model == null || model.isBlank()) {
            throw new AssistantIndisponibleException(
                    "Les services vocaux de l'assistant IA ne sont pas configurés.");
        }
        try {
            URI uri = URI.create(endpoint);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                throw new AssistantIndisponibleException("L'endpoint vocal doit être une URL HTTPS valide.");
            }
        } catch (IllegalArgumentException exception) {
            throw new AssistantIndisponibleException("L'endpoint vocal doit être une URL HTTPS valide.", exception);
        }
    }

    private byte[] readAudio(MultipartFile audio) {
        try {
            return audio.getBytes();
        } catch (java.io.IOException exception) {
            throw new IllegalArgumentException("Impossible de lire le fichier audio transmis", exception);
        }
    }

    private record TranscriptionProviderResponse(String text) {
    }
}
