package sn.yay360.api.helper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sn.yay360.api.dto.DashboardDto;
import sn.yay360.api.model.EtablissementSante;
import sn.yay360.api.model.RoleUtilisateur;
import sn.yay360.api.model.StatutGrossesse;
import sn.yay360.api.model.Utilisateur;
import sn.yay360.api.repo.AlerteMedicaleRepository;
import sn.yay360.api.repo.ConsultationRepository;
import sn.yay360.api.repo.EtablissementSanteRepository;
import sn.yay360.api.repo.GrossesseRepository;
import sn.yay360.api.repo.RendezVousRepository;
import sn.yay360.api.repo.UtilisateurRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/** Tableau de bord : toute la plateforme pour l'administrateur, son seul établissement pour l'administrateur d'établissement. */
class DashboardHelperTest {

    private UtilisateurRepository utilisateurRepository;
    private GrossesseRepository grossesseRepository;
    private ConsultationRepository consultationRepository;
    private RendezVousRepository rendezVousRepository;
    private AlerteMedicaleRepository alerteRepository;
    private EtablissementSanteRepository etablissementRepository;
    private DashboardHelper helper;

    @BeforeEach
    void setUp() {
        utilisateurRepository = mock(UtilisateurRepository.class);
        grossesseRepository = mock(GrossesseRepository.class);
        consultationRepository = mock(ConsultationRepository.class);
        rendezVousRepository = mock(RendezVousRepository.class);
        alerteRepository = mock(AlerteMedicaleRepository.class);
        etablissementRepository = mock(EtablissementSanteRepository.class);
        helper = new DashboardHelper(utilisateurRepository, grossesseRepository, consultationRepository,
                rendezVousRepository, alerteRepository, etablissementRepository, new CalculObstetricalHelper(),
                new PerimetreAdminHelper(utilisateurRepository, etablissementRepository));

        when(etablissementRepository.activiteEtablissementsActifs()).thenReturn(List.of(
                new Object[]{1L, "Hôpital Principal de Dakar", "Dakar", 12L},
                new Object[]{2L, "Centre de Santé Roi Baudouin", "Dakar", 5L}));
    }

    private Utilisateur compte(String email, String role, EtablissementSante administre) {
        Utilisateur u = new Utilisateur();
        u.setEmail(email);
        u.setRole(role);
        u.setEtablissementAdministre(administre);
        when(utilisateurRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(u));
        return u;
    }

    private static Object[] ligne(String cle, long valeur) {
        return new Object[]{cle, valeur};
    }

    @Test
    void lAdministrateurDeLaPlateformeVoitTouteLaPlateforme() {
        compte("admin@yay360.sn", RoleUtilisateur.ADMIN, null);
        when(utilisateurRepository.compterParRole()).thenReturn(List.<Object[]>of(
                ligne(RoleUtilisateur.FEMME_ENCEINTE, 10), ligne(RoleUtilisateur.MEDECIN, 6), ligne(RoleUtilisateur.ADMIN, 1)));
        when(grossesseRepository.compterParStatut()).thenReturn(List.<Object[]>of(ligne(StatutGrossesse.EN_COURS, 8)));
        when(grossesseRepository.countByStatutAndGrossesseARisqueTrue(StatutGrossesse.EN_COURS)).thenReturn(2L);
        when(consultationRepository.count()).thenReturn(30L);

        DashboardDto dto = helper.genererTableauDeBord("admin@yay360.sn");

        assertThat(dto.getNombreFemmesEnceintes()).isEqualTo(10);
        assertThat(dto.getNombreMedecins()).isEqualTo(6);
        assertThat(dto.getNombreGrossessesActives()).isEqualTo(8);
        assertThat(dto.getRatioGrossessesARisque()).isEqualTo(25.0);
        assertThat(dto.getNombreConsultations()).isEqualTo(30);
        assertThat(dto.getNombreEtablissementsActifs()).isEqualTo(2);
        verify(grossesseRepository, never()).compterParStatutDansEtablissement(anyLong());
    }

    @Test
    void lAdministrateurDEtablissementNeVoitQueSonEtablissement() {
        EtablissementSante hopital = new EtablissementSante();
        hopital.setId(1L);
        hopital.getProfessionnels().addAll(List.of(soignant(RoleUtilisateur.MEDECIN), soignant(RoleUtilisateur.SAGE_FEMME),
                soignant(RoleUtilisateur.SAGE_FEMME)));
        compte("admin.hpd@yay360.sn", RoleUtilisateur.ADMIN_ETABLISSEMENT, hopital);
        when(grossesseRepository.compterPatientesDansEtablissement(1L)).thenReturn(4L);
        when(grossesseRepository.compterParStatutDansEtablissement(1L)).thenReturn(List.<Object[]>of(
                ligne(StatutGrossesse.EN_COURS, 4), ligne(StatutGrossesse.TERMINEE, 1)));
        when(grossesseRepository.countByStatutAndGrossesseARisqueTrueAndEtablissementSuiviId(StatutGrossesse.EN_COURS, 1L))
                .thenReturn(1L);
        when(grossesseRepository.datesDernieresReglesEnCoursDansEtablissement(1L))
                .thenReturn(List.of(LocalDate.now().minusWeeks(8), LocalDate.now().minusWeeks(30)));
        when(consultationRepository.countByEtablissementId(1L)).thenReturn(9L);
        when(rendezVousRepository.compterParStatutDansEtablissement(1L)).thenReturn(List.<Object[]>of(ligne("PLANIFIE", 3)));
        when(alerteRepository.compterParStatutDansEtablissement(1L)).thenReturn(List.<Object[]>of(
                ligne("RESOLUE", 1), ligne("NOUVELLE", 1)));
        when(alerteRepository.compterNonResoluesParGraviteDansEtablissement(1L)).thenReturn(List.<Object[]>of(ligne("URGENT", 1)));
        LocalDateTime t = LocalDateTime.of(2026, 11, 20, 10, 0);
        when(alerteRepository.delaisPriseEnChargeDansEtablissement(1L)).thenReturn(List.<Object[]>of(new Object[]{t, t.plusMinutes(12)}));

        DashboardDto dto = helper.genererTableauDeBord("admin.hpd@yay360.sn");

        assertThat(dto.getNombreMedecins()).isEqualTo(1);
        assertThat(dto.getNombreSagesFemmes()).isEqualTo(2);
        assertThat(dto.getNombreFemmesEnceintes()).isEqualTo(4);
        assertThat(dto.getNombreAdmins()).isEqualTo(1);
        assertThat(dto.getNombreGrossessesActives()).isEqualTo(4);
        assertThat(dto.getNombreGrossessesTerminees()).isEqualTo(1);
        assertThat(dto.getRatioGrossessesARisque()).isEqualTo(25.0);
        assertThat(dto.getRepartitionGrossessesParTrimestre()).containsEntry("T1", 1L).containsEntry("T3", 1L);
        assertThat(dto.getNombreConsultations()).isEqualTo(9);
        assertThat(dto.getNombreRendezVous()).isEqualTo(3);
        assertThat(dto.getNombreAlertesUrgentes()).isEqualTo(1);
        assertThat(dto.getTauxResolutionAlertes()).isEqualTo(50.0);
        assertThat(dto.getDelaiMoyenPriseEnChargeMinutes()).isEqualTo(12.0);
        assertThat(dto.getActiviteEtablissements()).singleElement()
                .satisfies(a -> assertThat(a.getEtablissementId()).isEqualTo(1L));
        // Aucune requête sur l'ensemble de la plateforme
        verify(utilisateurRepository, never()).compterParRole();
        verify(grossesseRepository, never()).compterParStatut();
        verify(consultationRepository, never()).count();
        verify(alerteRepository, never()).compterParStatut();
    }

    private static Utilisateur soignant(String role) {
        Utilisateur u = new Utilisateur();
        u.setRole(role);
        return u;
    }
}
