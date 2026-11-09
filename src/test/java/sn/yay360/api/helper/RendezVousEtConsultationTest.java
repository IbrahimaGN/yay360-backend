package sn.yay360.api.helper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import sn.yay360.api.dto.ChangerStatutRendezVousRequest;
import sn.yay360.api.dto.EnregistrerConsultationRequest;
import sn.yay360.api.dto.ConsultationDto;
import sn.yay360.api.dto.PlanifierRendezVousRequest;
import sn.yay360.api.exception.OperationNonAutoriseeException;
import sn.yay360.api.mapper.ConsultationMapper;
import sn.yay360.api.mapper.RendezVousMapper;
import sn.yay360.api.model.AlerteMedicale;
import sn.yay360.api.model.Consultation;
import sn.yay360.api.model.Grossesse;
import sn.yay360.api.model.NiveauGravite;
import sn.yay360.api.model.RendezVous;
import sn.yay360.api.model.RoleUtilisateur;
import sn.yay360.api.model.StatutAlerte;
import sn.yay360.api.model.StatutGrossesse;
import sn.yay360.api.model.StatutRendezVous;
import sn.yay360.api.model.Utilisateur;
import sn.yay360.api.repo.AlerteMedicaleRepository;
import sn.yay360.api.repo.ConsultationRepository;
import sn.yay360.api.repo.RendezVousRepository;
import sn.yay360.api.repo.UtilisateurRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Règles métier des rendez-vous (transitions, disponibilité) et des consultations (cohérence, analyse). */
class RendezVousEtConsultationTest {

    private RendezVousRepository rendezVousRepository;
    private ConsultationRepository consultationRepository;
    private AlerteMedicaleRepository alerteRepository;
    private GrossesseHelper grossesseHelper;
    private AccesDossierHelper acces;
    private RendezVousHelper rendezVousHelper;
    private ConsultationHelper consultationHelper;

    private Utilisateur medecin;
    private Grossesse grossesse;

    @BeforeEach
    void setUp() {
        rendezVousRepository = mock(RendezVousRepository.class);
        consultationRepository = mock(ConsultationRepository.class);
        alerteRepository = mock(AlerteMedicaleRepository.class);
        grossesseHelper = mock(GrossesseHelper.class);
        acces = mock(AccesDossierHelper.class);
        UtilisateurRepository utilisateurRepository = mock(UtilisateurRepository.class);
        NotificationHelper notificationHelper = mock(NotificationHelper.class);

        rendezVousHelper = new RendezVousHelper(rendezVousRepository, utilisateurRepository, grossesseHelper,
                new RendezVousMapper(), mock(DossierFamilialHelper.class), acces, notificationHelper);
        consultationHelper = new ConsultationHelper(consultationRepository, rendezVousRepository, grossesseHelper,
                new ConsultationMapper(), mock(DossierFamilialHelper.class), acces, new MoteurReglesCliniques(),
                new CalculObstetricalHelper(), alerteRepository, utilisateurRepository, notificationHelper);

        medecin = new Utilisateur();
        medecin.setId(50L);
        medecin.setRole(RoleUtilisateur.MEDECIN);
        medecin.setPrenom("Fatou");
        medecin.setNom("Sarr");
        Utilisateur femme = new Utilisateur();
        femme.setId(1L);
        femme.setPrenom("Awa");
        femme.setNom("Diop");
        grossesse = new Grossesse();
        grossesse.setId(10L);
        grossesse.setFemmeEnceinte(femme);
        grossesse.setStatut(StatutGrossesse.EN_COURS);
        grossesse.setDateDernieresRegles(LocalDate.now().minusWeeks(30));

        when(acces.utilisateur("dr@yay360.sn")).thenReturn(medecin);
        when(grossesseHelper.trouverParId(10L)).thenReturn(grossesse);
        when(rendezVousRepository.save(any(RendezVous.class))).thenAnswer(i -> i.getArgument(0));
        when(consultationRepository.save(any(Consultation.class))).thenAnswer(i -> i.getArgument(0));
    }

    private RendezVous rdv(String statut, LocalDateTime date) {
        RendezVous rdv = new RendezVous();
        rdv.setId(7L);
        rdv.setGrossesse(grossesse);
        rdv.setFemmeEnceinte(grossesse.getFemmeEnceinte());
        rdv.setStatut(statut);
        rdv.setDateHeure(date);
        rdv.setType("CONSULTATION_PRENATALE");
        when(rendezVousRepository.findById(7L)).thenReturn(Optional.of(rdv));
        return rdv;
    }

    private ChangerStatutRendezVousRequest statut(String statut) {
        ChangerStatutRendezVousRequest request = new ChangerStatutRendezVousRequest();
        request.setStatut(statut);
        return request;
    }

    // ==================== RENDEZ-VOUS ====================

    @ParameterizedTest(name = "{0} → {1} autorisé : {2}")
    @CsvSource({
            "PLANIFIE, CONFIRME, true",
            "PLANIFIE, ANNULE, true",
            "CONFIRME, ABSENT, true",
            "CONFIRME, TERMINE, true",
            "ANNULE, CONFIRME, false",
            "TERMINE, CONFIRME, false",
            "ABSENT, TERMINE, false"
    })
    void matriceDesTransitionsDeStatut(String depart, String arrivee, boolean autorise) {
        RendezVous rdv = rdv(depart, LocalDateTime.now().minusDays(1));
        if (autorise) {
            rendezVousHelper.changerStatut("dr@yay360.sn", 7L, statut(arrivee));
            assertThat(rdv.getStatut()).isEqualTo(arrivee);
        } else {
            assertThatThrownBy(() -> rendezVousHelper.changerStatut("dr@yay360.sn", 7L, statut(arrivee)))
                    .isInstanceOf(OperationNonAutoriseeException.class)
                    .hasMessageContaining("Transition interdite");
        }
    }

    @Test
    void unRendezVousFuturNePeutPasEtreMarqueAbsent() {
        rdv(StatutRendezVous.CONFIRME, LocalDateTime.now().plusDays(2));

        assertThatThrownBy(() -> rendezVousHelper.changerStatut("dr@yay360.sn", 7L, statut(StatutRendezVous.ABSENT)))
                .isInstanceOf(OperationNonAutoriseeException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void planifier_exigeQueLeSoignantSuiveLaPatiente() {
        doThrow(new OperationNonAutoriseeException("Vous ne suivez pas cette patiente"))
                .when(acces).verifierSoin(medecin, grossesse);
        PlanifierRendezVousRequest request = new PlanifierRendezVousRequest();
        request.setGrossesseId(10L);
        request.setDateHeure(LocalDateTime.now().plusDays(3));
        request.setType("ECHOGRAPHIE");

        assertThatThrownBy(() -> rendezVousHelper.planifier("dr@yay360.sn", request))
                .isInstanceOf(OperationNonAutoriseeException.class);
        verify(rendezVousRepository, never()).save(any());
    }

    @Test
    void planifier_refuseUnCreneauDejaOccupe() {
        LocalDateTime creneau = LocalDateTime.now().plusDays(3).withNano(0);
        when(rendezVousRepository.existsByProfessionnelIdAndDateHeureAndStatutIn(50L, creneau,
                RendezVousHelper.STATUTS_ACTIFS)).thenReturn(true);
        PlanifierRendezVousRequest request = new PlanifierRendezVousRequest();
        request.setGrossesseId(10L);
        request.setDateHeure(creneau);
        request.setType("ECHOGRAPHIE");

        assertThatThrownBy(() -> rendezVousHelper.planifier("dr@yay360.sn", request))
                .isInstanceOf(OperationNonAutoriseeException.class)
                .hasMessageContaining("déjà un rendez-vous");
    }

    // ==================== CONSULTATIONS ====================

    private EnregistrerConsultationRequest consultation(Long rdvId) {
        EnregistrerConsultationRequest request = new EnregistrerConsultationRequest();
        request.setGrossesseId(10L);
        request.setRendezVousId(rdvId);
        request.setTensionArterielle("118/76");
        return request;
    }

    @Test
    void consultation_refuseLeRendezVousDUneAutreGrossesse() {
        RendezVous rdv = rdv(StatutRendezVous.CONFIRME, LocalDateTime.now());
        Grossesse autre = new Grossesse();
        autre.setId(11L);
        rdv.setGrossesse(autre);

        assertThatThrownBy(() -> consultationHelper.enregistrer("dr@yay360.sn", consultation(7L)))
                .isInstanceOf(OperationNonAutoriseeException.class)
                .hasMessageContaining("ne concerne pas");
        assertThat(rdv.getStatut()).isEqualTo(StatutRendezVous.CONFIRME);
    }

    @Test
    void consultation_refuseUnRendezVousAnnule() {
        rdv(StatutRendezVous.ANNULE, LocalDateTime.now());

        assertThatThrownBy(() -> consultationHelper.enregistrer("dr@yay360.sn", consultation(7L)))
                .isInstanceOf(OperationNonAutoriseeException.class);
    }

    @Test
    void consultation_cloturelAutomatiquementLeRendezVousHonore() {
        RendezVous rdv = rdv(StatutRendezVous.CONFIRME, LocalDateTime.now());

        ConsultationDto dto = consultationHelper.enregistrer("dr@yay360.sn", consultation(7L));

        assertThat(rdv.getStatut()).isEqualTo(StatutRendezVous.TERMINE);
        assertThat(dto.getSignauxCliniques()).isEmpty();
        verify(alerteRepository, never()).save(any());
    }

    @Test
    void consultation_constantesAnormalesTraceesEnAlerteAutomatique() {
        EnregistrerConsultationRequest request = consultation(null);
        request.setTensionArterielle("162/104");

        ConsultationDto dto = consultationHelper.enregistrer("dr@yay360.sn", request);

        assertThat(dto.getSignauxCliniques()).singleElement().asString().contains("hypertension sévère");
        verify(alerteRepository).save(argThat((AlerteMedicale a) ->
                NiveauGravite.URGENT.equals(a.getNiveauGravite())
                        && StatutAlerte.PRISE_EN_CHARGE.equals(a.getStatut())
                        && a.getProfessionnelPriseEnCharge() == medecin
                        && ConsultationHelper.ORIGINE_ANALYSE_AUTOMATIQUE.equals(a.getOrigine())));
    }

    @Test
    void consultation_enregistreLesConstantesComplementairesEtLesAnalyse() {
        EnregistrerConsultationRequest request = consultation(null);
        request.setFrequenceCardiaqueMaternelle(118);
        request.setProteinurie("NEGATIVE");
        request.setGlycosurie("UNE_CROIX");
        // Glycosurie déjà positive à une consultation précédente : règle NICE NG3 (≥ + répétée)
        when(consultationRepository.countByGrossesseIdAndGlycosurieIn(eq(grossesse.getId()), any())).thenReturn(1L);

        ConsultationDto dto = consultationHelper.enregistrer("dr@yay360.sn", request);

        assertThat(dto.getFrequenceCardiaqueMaternelle()).isEqualTo(118);
        assertThat(dto.getProteinurie()).isEqualTo("NEGATIVE");
        assertThat(dto.getGlycosurie()).isEqualTo("UNE_CROIX");
        assertThat(dto.getSignauxCliniques()).hasSize(2)
                .anySatisfy(sig -> assertThat(sig).contains("diabète gestationnel"))
                .anySatisfy(sig -> assertThat(sig).contains("118/min"));
        verify(alerteRepository).save(argThat((AlerteMedicale a) -> NiveauGravite.MODERE.equals(a.getNiveauGravite())));
    }

    @Test
    void consultation_refuseUneGrossesseCloturee() {
        grossesse.setStatut(StatutGrossesse.TERMINEE);

        assertThatThrownBy(() -> consultationHelper.enregistrer("dr@yay360.sn", consultation(null)))
                .isInstanceOf(OperationNonAutoriseeException.class);
        verify(consultationRepository, never()).save(any());
    }

    @Test
    void pourcentagesDuTableauDeBord() {
        assertThat(DashboardHelper.pourcentage(1, 3)).isEqualTo(33.3);
        assertThat(DashboardHelper.pourcentage(0, 0)).isZero();
        assertThat(DashboardHelper.pourcentage(2, 2)).isEqualTo(100.0);
        assertThat(List.of()).isEmpty();
    }
}
