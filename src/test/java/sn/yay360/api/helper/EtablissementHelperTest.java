package sn.yay360.api.helper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sn.yay360.api.dto.AffecterProfessionnelRequest;
import sn.yay360.api.dto.EtablissementDto;
import sn.yay360.api.dto.EtablissementRequest;
import sn.yay360.api.exception.OperationNonAutoriseeException;
import sn.yay360.api.exception.ResourceNotFoundException;
import sn.yay360.api.mapper.EtablissementMapper;
import sn.yay360.api.model.EtablissementSante;
import sn.yay360.api.model.RoleUtilisateur;
import sn.yay360.api.model.Utilisateur;
import sn.yay360.api.repo.EtablissementSanteRepository;
import sn.yay360.api.repo.UtilisateurRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Périmètre des administrateurs sur les établissements (plateforme : tous ; établissement : le sien). */
class EtablissementHelperTest {

    private static final String EMAIL_ADMIN = "admin@yay360.sn";
    private static final String EMAIL_ADMIN_HPD = "admin.hpd@yay360.sn";
    private static final String EMAIL_MEDECIN = "medecin@yay360.sn";

    private UtilisateurRepository utilisateurRepository;
    private EtablissementSanteRepository etablissementRepository;
    private EtablissementHelper helper;
    private EtablissementSante hopitalPrincipal;
    private EtablissementSante roiBaudouin;
    private Utilisateur medecin;

    @BeforeEach
    void setUp() {
        utilisateurRepository = mock(UtilisateurRepository.class);
        etablissementRepository = mock(EtablissementSanteRepository.class);
        helper = new EtablissementHelper(etablissementRepository, utilisateurRepository, new EtablissementMapper(),
                new PerimetreAdminHelper(utilisateurRepository, etablissementRepository));
        when(etablissementRepository.save(any(EtablissementSante.class))).thenAnswer(i -> i.getArgument(0));

        hopitalPrincipal = etablissement(1L, "Hôpital Principal de Dakar");
        roiBaudouin = etablissement(2L, "Centre de Santé Roi Baudouin");
        compte(10L, EMAIL_ADMIN, RoleUtilisateur.ADMIN, null);
        compte(11L, EMAIL_ADMIN_HPD, RoleUtilisateur.ADMIN_ETABLISSEMENT, hopitalPrincipal);
        medecin = compte(20L, EMAIL_MEDECIN, RoleUtilisateur.MEDECIN, null);
        medecin.setPrenom("Mamadou");
        medecin.setNom("Diallo");
    }

    private EtablissementSante etablissement(Long id, String nom) {
        EtablissementSante e = new EtablissementSante();
        e.setId(id);
        e.setNom(nom);
        e.setActif(true);
        when(etablissementRepository.findById(id)).thenReturn(Optional.of(e));
        when(etablissementRepository.existsById(id)).thenReturn(true);
        return e;
    }

    private Utilisateur compte(Long id, String email, String role, EtablissementSante administre) {
        Utilisateur u = new Utilisateur();
        u.setId(id);
        u.setEmail(email);
        u.setRole(role);
        u.setEtablissementAdministre(administre);
        when(utilisateurRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(u));
        when(utilisateurRepository.findById(id)).thenReturn(Optional.of(u));
        return u;
    }

    private static EtablissementRequest coordonnees(String nom) {
        EtablissementRequest request = new EtablissementRequest();
        request.setNom(nom);
        request.setType("HOPITAL");
        request.setAdresse("Dakar");
        request.setTelephone("+221338000000");
        return request;
    }

    private static AffecterProfessionnelRequest parEmail(String email) {
        AffecterProfessionnelRequest request = new AffecterProfessionnelRequest();
        request.setEmail(email);
        return request;
    }

    // ==================== MON ÉTABLISSEMENT ====================

    @Test
    void monEtablissement_renvoieCeluiQueGereLAdministrateur() {
        assertThat(helper.monEtablissement(EMAIL_ADMIN_HPD).getId()).isEqualTo(1L);
    }

    @Test
    void monEtablissement_refuseUnCompteSansEtablissement() {
        assertThatThrownBy(() -> helper.monEtablissement(EMAIL_ADMIN))
                .isInstanceOf(OperationNonAutoriseeException.class);
    }

    // ==================== MODIFICATION ====================

    @Test
    void lAdministrateurDEtablissementModifieLeSien() {
        EtablissementDto dto = helper.mettreAJour(EMAIL_ADMIN_HPD, 1L, coordonnees("HPD rénové"));

        assertThat(dto.getNom()).isEqualTo("HPD rénové");
    }

    @Test
    void lAdministrateurDEtablissementNeModifiePasUnAutre() {
        assertThatThrownBy(() -> helper.mettreAJour(EMAIL_ADMIN_HPD, 2L, coordonnees("Pris")))
                .isInstanceOf(OperationNonAutoriseeException.class);
        assertThat(roiBaudouin.getNom()).isEqualTo("Centre de Santé Roi Baudouin");
    }

    @Test
    void lAdministrateurDeLaPlateformeModifieTousLesEtablissements() {
        assertThat(helper.mettreAJour(EMAIL_ADMIN, 2L, coordonnees("CS Roi Baudouin")).getNom())
                .isEqualTo("CS Roi Baudouin");
    }

    @Test
    void unSoignantNeGereAucunEtablissement() {
        assertThatThrownBy(() -> helper.mettreAJour(EMAIL_MEDECIN, 1L, coordonnees("X")))
                .isInstanceOf(OperationNonAutoriseeException.class);
    }

    // ==================== AFFECTATION ET RETRAIT ====================

    @Test
    void affecter_parEmail_ajouteLeSoignantASonEtablissement() {
        helper.affecterProfessionnel(EMAIL_ADMIN_HPD, 1L, parEmail(" Medecin@Yay360.sn "));

        assertThat(hopitalPrincipal.getProfessionnels()).containsExactly(medecin);
    }

    @Test
    void affecter_parId_sansDoublon() {
        AffecterProfessionnelRequest request = new AffecterProfessionnelRequest();
        request.setProfessionnelId(20L);

        helper.affecterProfessionnel(EMAIL_ADMIN_HPD, 1L, request);
        helper.affecterProfessionnel(EMAIL_ADMIN_HPD, 1L, request);

        assertThat(hopitalPrincipal.getProfessionnels()).containsExactly(medecin);
    }

    @Test
    void affecter_unEmailNonSoignantRepondCommeUnEmailInconnu() {
        compte(30L, "awa@exemple.sn", RoleUtilisateur.FEMME_ENCEINTE, null);

        assertThatThrownBy(() -> helper.affecterProfessionnel(EMAIL_ADMIN_HPD, 1L, parEmail("awa@exemple.sn")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Aucun soignant n'a cet email");
        assertThatThrownBy(() -> helper.affecterProfessionnel(EMAIL_ADMIN_HPD, 1L, parEmail("inconnu@exemple.sn")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Aucun soignant n'a cet email");
    }

    @Test
    void affecter_parIdRefuseUnCompteQuiNEstPasSoignant() {
        AffecterProfessionnelRequest request = new AffecterProfessionnelRequest();
        request.setProfessionnelId(11L);

        assertThatThrownBy(() -> helper.affecterProfessionnel(EMAIL_ADMIN_HPD, 1L, request))
                .isInstanceOf(OperationNonAutoriseeException.class);
    }

    @Test
    void affecter_exigeUnIdOuUnEmail() {
        assertThatThrownBy(() -> helper.affecterProfessionnel(EMAIL_ADMIN_HPD, 1L, new AffecterProfessionnelRequest()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> helper.affecterProfessionnel(EMAIL_ADMIN_HPD, 1L, parEmail("  ")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void affecter_unIdInconnuEstSignale() {
        AffecterProfessionnelRequest request = new AffecterProfessionnelRequest();
        request.setProfessionnelId(999L);

        assertThatThrownBy(() -> helper.affecterProfessionnel(EMAIL_ADMIN_HPD, 1L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void lAdministrateurDEtablissementNAffectePasDansUnAutre() {
        assertThatThrownBy(() -> helper.affecterProfessionnel(EMAIL_ADMIN_HPD, 2L, parEmail(EMAIL_MEDECIN)))
                .isInstanceOf(OperationNonAutoriseeException.class);
        assertThat(roiBaudouin.getProfessionnels()).isEmpty();
    }

    @Test
    void retirer_enleveLeSoignantDeSonEtablissementSeulement() {
        hopitalPrincipal.getProfessionnels().add(medecin);
        roiBaudouin.getProfessionnels().add(medecin);

        helper.retirerProfessionnel(EMAIL_ADMIN_HPD, 1L, 20L);

        assertThat(hopitalPrincipal.getProfessionnels()).isEmpty();
        assertThat(roiBaudouin.getProfessionnels()).containsExactly(medecin);
        assertThatThrownBy(() -> helper.retirerProfessionnel(EMAIL_ADMIN_HPD, 2L, 20L))
                .isInstanceOf(OperationNonAutoriseeException.class);
    }

    // ==================== SUPPRESSION ====================

    @Test
    void supprimer_refuseUnEtablissementQuiAUnAdministrateur() {
        when(utilisateurRepository.existsByEtablissementAdministreId(1L)).thenReturn(true);

        assertThatThrownBy(() -> helper.supprimer(1L))
                .isInstanceOf(OperationNonAutoriseeException.class)
                .hasMessageContaining("administrateur");
        verify(etablissementRepository, never()).deleteById(any());
    }

    @Test
    void supprimer_unEtablissementSansAdministrateur() {
        helper.supprimer(2L);

        verify(etablissementRepository).deleteById(2L);
    }

    @Test
    void supprimer_unEtablissementInconnuEstSignale() {
        assertThatThrownBy(() -> helper.supprimer(99L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
