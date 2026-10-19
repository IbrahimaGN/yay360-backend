package sn.yay360.api.helper;

import sn.yay360.api.config.AssistantIaProperties;
import sn.yay360.api.dto.AssistantChatRequest;
import sn.yay360.api.dto.AssistantTourRequest;
import sn.yay360.api.exception.AssistantIndisponibleException;
import sn.yay360.api.exception.OperationNonAutoriseeException;
import sn.yay360.api.model.Utilisateur;
import sn.yay360.api.repo.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AssistantIaHelperTest {

    private RestClient restClient;
    private AssistantIaProperties properties;
    private UtilisateurRepository utilisateurRepository;
    private AssistantIaHelper helper;

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        properties = new AssistantIaProperties();
        utilisateurRepository = mock(UtilisateurRepository.class);
        helper = new AssistantIaHelper(restClient, properties, utilisateurRepository);

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail("awa@example.com");
        when(utilisateurRepository.findByEmail("awa@example.com")).thenReturn(Optional.of(utilisateur));
    }

    @Test
    void repondre_refuseLaTransmissionSansConsentement() {
        AssistantChatRequest request = requete();
        request.setConsentementTransmission(false);

        assertThatThrownBy(() -> helper.repondre("awa@example.com", request))
                .isInstanceOf(OperationNonAutoriseeException.class)
                .hasMessageContaining("consentement explicite");

        verifyNoInteractions(restClient);
    }

    @Test
    void repondreSignaleAssistantNonConfigureSansAppelerLeFournisseur() {
        AssistantChatRequest request = requete();
        request.setConsentementTransmission(true);

        assertThatThrownBy(() -> helper.repondre("awa@example.com", request))
                .isInstanceOf(AssistantIndisponibleException.class)
                .hasMessageContaining("pas configuré");

        verifyNoInteractions(restClient);
    }

    @Test
    void repondreRefuseUnHistoriqueIncompletSansAppelerLeFournisseur() {
        properties.setEnabled(true);
        properties.setEndpoint("https://example.com/v1/chat/completions");
        properties.setApiKey("test-key");
        properties.setModel("test-model");
        AssistantChatRequest request = requete();
        request.setConsentementTransmission(true);
        AssistantTourRequest tour = new AssistantTourRequest();
        tour.setRole("user");
        tour.setContent("Bonjour");
        request.setHistorique(List.of(tour));

        assertThatThrownBy(() -> helper.repondre("awa@example.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("par paires");

        verifyNoInteractions(restClient);
    }

    private AssistantChatRequest requete() {
        AssistantChatRequest request = new AssistantChatRequest();
        request.setMessage("J'ai une question");
        request.setCanal("TEXTE");
        request.setHistorique(List.of());
        return request;
    }
}
