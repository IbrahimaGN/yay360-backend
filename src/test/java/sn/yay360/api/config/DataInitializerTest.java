package sn.yay360.api.config;

import sn.yay360.api.helper.CalculObstetricalHelper;
import sn.yay360.api.model.AlerteMedicale;
import sn.yay360.api.model.Consultation;
import sn.yay360.api.model.EtapeGrossesse;
import sn.yay360.api.model.Grossesse;
import sn.yay360.api.model.Notification;
import sn.yay360.api.model.RendezVous;
import sn.yay360.api.model.RoleUtilisateur;
import sn.yay360.api.model.Utilisateur;
import sn.yay360.api.repo.ConseilGrossesseRepository;
import sn.yay360.api.repo.AlerteMedicaleRepository;
import sn.yay360.api.repo.ConsultationRepository;
import sn.yay360.api.repo.EtablissementSanteRepository;
import sn.yay360.api.repo.EtapeGrossesseRepository;
import sn.yay360.api.repo.GrossesseRepository;
import sn.yay360.api.repo.NotificationRepository;
import sn.yay360.api.repo.RendezVousRepository;
import sn.yay360.api.repo.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DataInitializerTest {

    private EtapeGrossesseRepository etapeGrossesseRepository;
    private UtilisateurRepository utilisateurRepository;
    private GrossesseRepository grossesseRepository;
    private RendezVousRepository rendezVousRepository;
    private ConsultationRepository consultationRepository;
    private AlerteMedicaleRepository alerteMedicaleRepository;
    private NotificationRepository notificationRepository;
    private DataInitializer initializer;

    @BeforeEach
    void setUp() {
        etapeGrossesseRepository = mock(EtapeGrossesseRepository.class);
        utilisateurRepository = mock(UtilisateurRepository.class);
        grossesseRepository = mock(GrossesseRepository.class);
        rendezVousRepository = mock(RendezVousRepository.class);
        consultationRepository = mock(ConsultationRepository.class);
        alerteMedicaleRepository = mock(AlerteMedicaleRepository.class);
        notificationRepository = mock(NotificationRepository.class);
        when(utilisateurRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(grossesseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(rendezVousRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(consultationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(alerteMedicaleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        initializer = new DataInitializer(
                utilisateurRepository,
                etapeGrossesseRepository,
                mock(ConseilGrossesseRepository.class),
                mock(EtablissementSanteRepository.class),
                grossesseRepository,
                rendezVousRepository,
                consultationRepository,
                alerteMedicaleRepository,
                notificationRepository,
                mock(PasswordEncoder.class),
                new CalculObstetricalHelper()
        );
    }

    @Test
    void seedEtapesGrossesseInitialiseQuaranteSemaines() {
        ArgumentCaptor<EtapeGrossesse> captor = ArgumentCaptor.forClass(EtapeGrossesse.class);

        initializer.seedEtapesGrossesse();

        verify(etapeGrossesseRepository, times(40)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(EtapeGrossesse::getSemaine)
                .containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1, 40).boxed().toList());
    }

    @Test
    void seedDonneesDemoCreeTousLesProfilsEtDonneesDeSuivi() {
        initializer.seedDonneesDemo();

        ArgumentCaptor<Utilisateur> utilisateurs = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository, times(5)).save(utilisateurs.capture());
        assertThat(utilisateurs.getAllValues())
                .extracting(Utilisateur::getRole)
                .containsExactlyInAnyOrder(
                        RoleUtilisateur.MEDECIN,
                        RoleUtilisateur.SAGE_FEMME,
                        RoleUtilisateur.FEMME_ENCEINTE,
                        RoleUtilisateur.FEMME_ENCEINTE,
                        RoleUtilisateur.CONJOINT
                );
        verify(grossesseRepository, times(2)).save(any(Grossesse.class));
        verify(rendezVousRepository, times(3)).save(any(RendezVous.class));
        verify(consultationRepository).save(any(Consultation.class));
        verify(alerteMedicaleRepository, times(2)).save(any(AlerteMedicale.class));
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

}
