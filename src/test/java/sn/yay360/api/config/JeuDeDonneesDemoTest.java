package sn.yay360.api.config;

import sn.yay360.api.helper.MoteurReglesCliniques;
import sn.yay360.api.model.AlerteMedicale;
import sn.yay360.api.model.Consultation;
import sn.yay360.api.model.EtablissementSante;
import sn.yay360.api.model.Grossesse;
import sn.yay360.api.model.RendezVous;
import sn.yay360.api.model.RoleUtilisateur;
import sn.yay360.api.model.StatutAlerte;
import sn.yay360.api.model.StatutGrossesse;
import sn.yay360.api.model.StatutRendezVous;
import sn.yay360.api.model.Utilisateur;
import sn.yay360.api.repo.AlerteMedicaleRepository;
import sn.yay360.api.repo.ConsultationRepository;
import sn.yay360.api.repo.EtablissementSanteRepository;
import sn.yay360.api.repo.GrossesseRepository;
import sn.yay360.api.repo.NotificationRepository;
import sn.yay360.api.repo.RendezVousRepository;
import sn.yay360.api.repo.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JeuDeDonneesDemoTest {

    private UtilisateurRepository utilisateurRepository;
    private GrossesseRepository grossesseRepository;
    private RendezVousRepository rendezVousRepository;
    private ConsultationRepository consultationRepository;
    private AlerteMedicaleRepository alerteMedicaleRepository;
    private NotificationRepository notificationRepository;
    private JeuDeDonneesDemo jeu;

    @BeforeEach
    void setUp() {
        utilisateurRepository = mock(UtilisateurRepository.class);
        EtablissementSanteRepository etablissementRepository = mock(EtablissementSanteRepository.class);
        grossesseRepository = mock(GrossesseRepository.class);
        rendezVousRepository = mock(RendezVousRepository.class);
        consultationRepository = mock(ConsultationRepository.class);
        alerteMedicaleRepository = mock(AlerteMedicaleRepository.class);
        notificationRepository = mock(NotificationRepository.class);

        List<EtablissementSante> etablissements = new ArrayList<>();
        for (String nom : JeuDeDonneesDemo.ETABLISSEMENTS) {
            EtablissementSante e = new EtablissementSante();
            e.setNom(nom);
            etablissements.add(e);
        }
        when(etablissementRepository.findAll()).thenReturn(etablissements);
        when(utilisateurRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(grossesseRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(rendezVousRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(consultationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(alerteMedicaleRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(notificationRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        jeu = new JeuDeDonneesDemo(utilisateurRepository, etablissementRepository, grossesseRepository,
                rendezVousRepository, consultationRepository, alerteMedicaleRepository, notificationRepository,
                mock(PasswordEncoder.class), new MoteurReglesCliniques(), mock(JdbcTemplate.class));
        ReflectionTestUtils.setField(jeu, "motDePasseDemo", "Demo@2026");
    }

    private List<Utilisateur> comptesCrees() {
        ArgumentCaptor<Utilisateur> captor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues();
    }

    @Test
    void creeSixMedecinsSixSagesFemmesDixPatientesEtDixConjoints() {
        jeu.charger();

        Map<String, Long> parRole = comptesCrees().stream()
                .collect(Collectors.groupingBy(Utilisateur::getRole, Collectors.counting()));
        assertThat(parRole).containsOnly(
                Map.entry(RoleUtilisateur.MEDECIN, 6L),
                Map.entry(RoleUtilisateur.SAGE_FEMME, 6L),
                Map.entry(RoleUtilisateur.FEMME_ENCEINTE, 10L),
                Map.entry(RoleUtilisateur.CONJOINT, 10L));
    }

    @Test
    void chaqueConjointEstLieAUneFemmeDifferente() {
        jeu.charger();

        List<Utilisateur> conjoints = comptesCrees().stream()
                .filter(u -> RoleUtilisateur.CONJOINT.equals(u.getRole())).toList();
        assertThat(conjoints).allSatisfy(c -> assertThat(c.getFemmeAssociee()).isNotNull());
        assertThat(conjoints.stream().map(Utilisateur::getFemmeAssociee).distinct()).hasSize(10);
    }

    @Test
    void lesComptesSontFictifsEtUniques() {
        jeu.charger();

        List<String> emails = comptesCrees().stream().map(Utilisateur::getEmail).toList();
        assertThat(emails).allSatisfy(e -> assertThat(e).endsWith(JeuDeDonneesDemo.SUFFIXE_EMAIL).matches("[a-z.]+@yay360\\.sn"));
        assertThat(emails).doesNotHaveDuplicates().contains("ndeye.fatou.diop.demo@yay360.sn");
        assertThat(comptesCrees().stream().map(Utilisateur::getTelephone)).doesNotHaveDuplicates()
                .allSatisfy(t -> assertThat(t).matches("^\\+221[0-9]{9}$"));
    }

    @Test
    void creeDixGrossessesEnCoursDontTroisARisqueEtDeuxGrossessesPassees() {
        jeu.charger();

        ArgumentCaptor<Grossesse> captor = ArgumentCaptor.forClass(Grossesse.class);
        verify(grossesseRepository, times(12)).save(captor.capture());
        List<Grossesse> enCours = captor.getAllValues().stream()
                .filter(g -> StatutGrossesse.EN_COURS.equals(g.getStatut())).toList();
        assertThat(enCours).hasSize(10);
        assertThat(enCours.stream().filter(Grossesse::getGrossesseARisque))
                .hasSize(3)
                .allSatisfy(g -> assertThat(g.getMotifRisque()).contains("fictive"));
        assertThat(enCours.stream().filter(g -> g.getEtablissementSuivi() == null)).hasSize(1);
        assertThat(captor.getAllValues().stream().filter(g -> StatutGrossesse.TERMINEE.equals(g.getStatut())))
                .hasSize(2).allSatisfy(g -> assertThat(g.getDateFin()).isNotNull());
    }

    @Test
    void lesConsultationsSuiventLesContactsOmsEtLeMoteurDeRegles() {
        jeu.charger();

        ArgumentCaptor<Consultation> captor = ArgumentCaptor.forClass(Consultation.class);
        verify(consultationRepository, atLeastOnce()).save(captor.capture());
        List<Consultation> consultations = captor.getAllValues();
        // Contacts OMS déjà passés pour les 9 patientes suivies (24, 8, 32, 16, 36, 12, 28, 20, 38 SA)
        assertThat(consultations).hasSize(2 + 0 + 4 + 1 + 5 + 0 + 3 + 1 + 6);
        assertThat(consultations).allSatisfy(c -> {
            assertThat(c.getRendezVous()).isNotNull();
            assertThat(c.getRendezVous().getStatut()).isEqualTo(StatutRendezVous.TERMINE);
            assertThat(c.getDiagnostic()).isNull();
        });
        // Les trois constantes illustratives produisent des signaux calculés par le moteur
        assertThat(consultations.stream().map(Consultation::getSignauxCliniques).filter(s -> s != null))
                .anySatisfy(s -> assertThat(s).startsWith("HTA|"))
                .anySatisfy(s -> assertThat(s).startsWith("FIEVRE|"))
                .anySatisfy(s -> assertThat(s).contains("HU_DISCORDANTE|"));
    }

    @Test
    void lesAlertesOntUnNiveauEtUneJustificationIssusDuMoteur() {
        jeu.charger();

        ArgumentCaptor<AlerteMedicale> captor = ArgumentCaptor.forClass(AlerteMedicale.class);
        verify(alerteMedicaleRepository, atLeastOnce()).saveAndFlush(captor.capture());
        List<AlerteMedicale> alertes = captor.getAllValues();
        assertThat(alertes).allSatisfy(a -> {
            assertThat(a.getNiveauGravite()).isNotBlank();
            assertThat(a.getJustificationTriage()).isNotBlank();
        });
        Map<String, Long> parOrigine = alertes.stream()
                .collect(Collectors.groupingBy(AlerteMedicale::getOrigine, Collectors.counting()));
        assertThat(parOrigine.get("PATIENTE")).isEqualTo(8L);
        assertThat(parOrigine.get("ANALYSE_AUTOMATIQUE")).isGreaterThanOrEqualTo(3L);
        assertThat(alertes.stream().map(AlerteMedicale::getStatut).collect(Collectors.toSet()))
                .contains(StatutAlerte.NOUVELLE, StatutAlerte.PRISE_EN_CHARGE, StatutAlerte.RESOLUE);
        assertThat(alertes.stream().filter(a -> !StatutAlerte.NOUVELLE.equals(a.getStatut())))
                .allSatisfy(a -> assertThat(a.getProfessionnelPriseEnCharge()).isNotNull());
    }

    @Test
    void lePlanningCouvreTousLesStatutsDeRendezVous() {
        jeu.charger();

        ArgumentCaptor<RendezVous> captor = ArgumentCaptor.forClass(RendezVous.class);
        verify(rendezVousRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues().stream().map(RendezVous::getStatut).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(StatutRendezVous.PLANIFIE, StatutRendezVous.CONFIRME,
                        StatutRendezVous.TERMINE, StatutRendezVous.ANNULE, StatutRendezVous.ABSENT);
    }

    @Test
    void chaqueEtablissementRecoitAuMoinsUnMedecinEtUneSageFemme() {
        jeu.charger();

        Map<String, EtablissementSante> parNom = new java.util.HashMap<>();
        ArgumentCaptor<RendezVous> rdv = ArgumentCaptor.forClass(RendezVous.class);
        verify(rendezVousRepository, atLeastOnce()).save(rdv.capture());
        rdv.getAllValues().stream().map(RendezVous::getEtablissement).filter(e -> e != null)
                .forEach(e -> parNom.put(e.getNom(), e));
        assertThat(parNom.keySet()).containsExactlyInAnyOrderElementsOf(JeuDeDonneesDemo.ETABLISSEMENTS);
        assertThat(parNom.values()).allSatisfy(e -> {
            Map<String, Long> roles = e.getProfessionnels().stream()
                    .collect(Collectors.groupingBy(Utilisateur::getRole, Collectors.counting()));
            assertThat(roles).containsKeys(RoleUtilisateur.MEDECIN, RoleUtilisateur.SAGE_FEMME);
        });
    }

    @Test
    void neRechargePasUnJeuDejaPresent() {
        when(utilisateurRepository.existsByEmailIgnoreCase(JeuDeDonneesDemo.EMAIL_TEMOIN)).thenReturn(true);

        jeu.charger();

        verify(utilisateurRepository, never()).save(any());
        verifyNoInteractions(grossesseRepository, consultationRepository, alerteMedicaleRepository);
    }
}
