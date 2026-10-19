package sn.yay360.api.helper;

import sn.yay360.api.config.AssistantIaProperties;
import sn.yay360.api.exception.AssistantIndisponibleException;
import sn.yay360.api.exception.OperationNonAutoriseeException;
import sn.yay360.api.model.Utilisateur;
import sn.yay360.api.repo.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AssistantAudioHelperTest {

    private RestClient restClient;
    private AssistantIaProperties properties;
    private UtilisateurRepository utilisateurRepository;
    private AssistantAudioHelper helper;

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        properties = new AssistantIaProperties();
        utilisateurRepository = mock(UtilisateurRepository.class);
        helper = new AssistantAudioHelper(restClient, properties, utilisateurRepository);
        when(utilisateurRepository.findByEmail("awa@example.com")).thenReturn(Optional.of(new Utilisateur()));
    }

    @Test
    void transcrireRefuseUneTransmissionSansConsentement() {
        MockMultipartFile audio = audioValide();

        assertThatThrownBy(() -> helper.transcrire("awa@example.com", audio, false))
                .isInstanceOf(OperationNonAutoriseeException.class)
                .hasMessageContaining("consentement explicite");

        verifyNoInteractions(restClient);
    }

    @Test
    void transcrireRefuseUnTypeAudioInvalideAvantAppelFournisseur() {
        configurerTranscription();
        MockMultipartFile fichier = new MockMultipartFile(
                "audio", "texte.txt", "text/plain", "pas un audio".getBytes());

        assertThatThrownBy(() -> helper.transcrire("awa@example.com", fichier, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Format audio");

        verifyNoInteractions(restClient);
    }

    @Test
    void transcrireRefuseUnAudioQuiDepasseLaLimiteConfiguree() {
        configurerTranscription();
        properties.setMaxAudioBytes(3);

        assertThatThrownBy(() -> helper.transcrire("awa@example.com", audioValide(), true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("taille maximale");

        verifyNoInteractions(restClient);
    }

    @Test
    void transcrireNAutorisePasUnEndpointNonHttps() {
        configurerTranscription();
        properties.setTranscriptionEndpoint("http://example.com/transcriptions");

        assertThatThrownBy(() -> helper.transcrire("awa@example.com", audioValide(), true))
                .isInstanceOf(AssistantIndisponibleException.class)
                .hasMessageContaining("HTTPS");

        verifyNoInteractions(restClient);
    }

    @Test
    void synthetiserRefuseUneTransmissionSansConsentement() {
        assertThatThrownBy(() -> helper.synthetiser("awa@example.com", "Bonjour", false))
                .isInstanceOf(OperationNonAutoriseeException.class)
                .hasMessageContaining("consentement explicite");

        verifyNoInteractions(restClient);
    }

    @Test
    void synthetiserRefuseUnTexteTropLongAvantAppelFournisseur() {
        properties.setEnabled(true);
        properties.setApiKey("test-key");
        properties.setSpeechEndpoint("https://example.com/v1/audio/speech");
        properties.setSpeechModel("test-speech-model");

        assertThatThrownBy(() -> helper.synthetiser("awa@example.com", "a".repeat(4001), true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("4000 caractères");

        verifyNoInteractions(restClient);
    }

    private void configurerTranscription() {
        properties.setEnabled(true);
        properties.setApiKey("test-key");
        properties.setTranscriptionEndpoint("https://example.com/v1/audio/transcriptions");
        properties.setTranscriptionModel("test-transcription-model");
    }

    private MockMultipartFile audioValide() {
        return new MockMultipartFile("audio", "voix.webm", "audio/webm", new byte[]{1, 2, 3, 4});
    }
}
