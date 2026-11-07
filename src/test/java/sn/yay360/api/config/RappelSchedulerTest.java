package sn.yay360.api.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import sn.yay360.api.dto.PlanCpnDto;
import sn.yay360.api.helper.CalculObstetricalHelper;
import sn.yay360.api.helper.IntelligenceCliniqueHelper;
import sn.yay360.api.helper.MoteurReglesCliniques;
import sn.yay360.api.helper.NotificationHelper;
import sn.yay360.api.model.Grossesse;
import sn.yay360.api.model.RendezVous;
import sn.yay360.api.model.TypeNotification;
import sn.yay360.api.model.Utilisateur;
import sn.yay360.api.repo.ConseilGrossesseRepository;
import sn.yay360.api.repo.GrossesseRepository;
import sn.yay360.api.repo.NotificationRepository;
import sn.yay360.api.repo.RendezVousRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RappelSchedulerTest {

    private RendezVousRepository rendezVousRepository;
    private NotificationHelper notificationHelper;
    private IntelligenceCliniqueHelper intelligenceClinique;
    private NotificationRepository notificationRepository;
    private RappelScheduler scheduler;
    private Grossesse grossesse;

    @BeforeEach
    void setUp() {
        rendezVousRepository = mock(RendezVousRepository.class);
        notificationHelper = mock(NotificationHelper.class);
        intelligenceClinique = mock(IntelligenceCliniqueHelper.class);
        notificationRepository = mock(NotificationRepository.class);
        scheduler = new RappelScheduler(rendezVousRepository, mock(GrossesseRepository.class),
                mock(ConseilGrossesseRepository.class), notificationHelper, new CalculObstetricalHelper(),
                mock(PlatformTransactionManager.class), intelligenceClinique, notificationRepository);

        Utilisateur femme = new Utilisateur();
        femme.setId(1L);
        grossesse = new Grossesse();
        grossesse.setId(10L);
        grossesse.setFemmeEnceinte(femme);
    }

    @Test
    void chaqueRendezVousDeLaFenetreEstRappeleUneFoisEtMarque() {
        RendezVous rdv = new RendezVous();
        rdv.setId(7L);
        rdv.setGrossesse(grossesse);
        rdv.setFemmeEnceinte(grossesse.getFemmeEnceinte());
        rdv.setType("ECHOGRAPHIE");
        rdv.setDateHeure(LocalDateTime.now().plusHours(20));
        when(rendezVousRepository.findARappeler(any(), any(), any())).thenReturn(List.of(rdv));
        when(rendezVousRepository.findById(7L)).thenReturn(Optional.of(rdv));

        scheduler.rappelerRendezVousAVenir();

        verify(notificationHelper).envoyer(eq(rdv.getFemmeEnceinte()), eq(grossesse),
                eq(TypeNotification.RAPPEL_ECHOGRAPHIE), anyString(), anyString());
        assertThat(rdv.getRappelEnvoye()).isTrue();
    }

    @Test
    void unEchecDEnvoiNEmpechePasLesAutresRappels() {
        when(rendezVousRepository.findARappeler(any(), any(), any())).thenReturn(List.of(rdvId(7L), rdvId(8L)));
        when(rendezVousRepository.findById(7L)).thenThrow(new IllegalStateException("panne"));
        RendezVous second = rdvId(8L);
        second.setGrossesse(grossesse);
        second.setFemmeEnceinte(grossesse.getFemmeEnceinte());
        second.setType("VACCINATION");
        second.setDateHeure(LocalDateTime.now().plusHours(5));
        when(rendezVousRepository.findById(8L)).thenReturn(Optional.of(second));

        scheduler.rappelerRendezVousAVenir();

        verify(notificationHelper).envoyer(any(), any(), eq(TypeNotification.RAPPEL_VACCINATION), anyString(), anyString());
    }

    @Test
    void uneConsultationPrenataleEnRetardNEstRappeleeQuUneFois() {
        var enRetard = new MoteurReglesCliniques.ContactCpn(2, 20, LocalDate.now().minusWeeks(4),
                MoteurReglesCliniques.CPN_EN_RETARD, null);
        var aVenir = new MoteurReglesCliniques.ContactCpn(3, 26, LocalDate.now().plusWeeks(2),
                MoteurReglesCliniques.CPN_A_VENIR, null);
        when(intelligenceClinique.planCpn(grossesse))
                .thenReturn(new PlanCpnDto(10L, 24, List.of(enRetard, aVenir), 1, 1, "OMS"));
        when(notificationRepository.existsByDestinataireIdAndGrossesseIdAndTitre(eq(1L), eq(10L), anyString()))
                .thenReturn(false, true);

        scheduler.rappelerCpnEnRetard(grossesse);
        scheduler.rappelerCpnEnRetard(grossesse);

        verify(notificationHelper, times(1)).envoyer(any(), eq(grossesse), eq(TypeNotification.RAPPEL_CONSULTATION),
                eq("Consultation prénatale n°2 à programmer"), anyString());
    }

    private static RendezVous rdvId(Long id) {
        RendezVous rdv = new RendezVous();
        rdv.setId(id);
        return rdv;
    }
}
