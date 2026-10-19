package sn.yay360.api.helper;

import sn.yay360.api.dto.DeclarerGrossesseRequest;
import sn.yay360.api.dto.DeclarerRisqueRequest;
import sn.yay360.api.dto.GrossesseDto;
import sn.yay360.api.dto.TerminerGrossesseRequest;
import sn.yay360.api.exception.OperationNonAutoriseeException;
import sn.yay360.api.exception.ResourceNotFoundException;
import sn.yay360.api.mapper.GrossesseMapper;
import sn.yay360.api.model.Grossesse;
import sn.yay360.api.model.RoleUtilisateur;
import sn.yay360.api.model.StatutGrossesse;
import sn.yay360.api.model.Utilisateur;
import sn.yay360.api.repo.GrossesseRepository;
import sn.yay360.api.repo.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Contient toute la logique métier liée à la déclaration et au suivi des grossesses (§5.3).
 */
@Component
@RequiredArgsConstructor
public class GrossesseHelper {

    private final GrossesseRepository grossesseRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final GrossesseMapper grossesseMapper;
    private final CalculObstetricalHelper calcul;
    private final DossierFamilialHelper dossierFamilialHelper;

    @PreAuthorize("hasAnyAuthority('FEMME_ENCEINTE', 'CONJOINT')")
    public GrossesseDto declarer(String emailFemme, DeclarerGrossesseRequest request) {
        Utilisateur femme = trouverUtilisateurParEmail(emailFemme);

        grossesseRepository.findByFemmeEnceinteIdAndStatut(femme.getId(), StatutGrossesse.EN_COURS)
                .ifPresent(g -> {
                    throw new OperationNonAutoriseeException("Une grossesse en cours existe déjà pour cette utilisatrice");
                });

        if (request.getDateDernieresRegles().isBefore(
                LocalDate.now().minusWeeks(calcul.dureeGrossesseSemaines() + 1L))) {
            throw new IllegalArgumentException("La date des dernières règles semble trop ancienne (> 40 semaines)");
        }

        Grossesse grossesse = new Grossesse();
        grossesse.setFemmeEnceinte(femme);
        grossesse.setDateDernieresRegles(request.getDateDernieresRegles());
        grossesse.setDatePrevueAccouchement(calcul.calculerDatePrevueAccouchement(request.getDateDernieresRegles()));
        grossesse.setNombreGrossessesPrecedentes(
                request.getNombreGrossessesPrecedentes() != null ? request.getNombreGrossessesPrecedentes() : 0);
        grossesse.setAntecedents(request.getAntecedents());
        grossesse.setStatut(StatutGrossesse.EN_COURS);
        grossesse.setGrossesseARisque(false);

        Grossesse enregistree = grossesseRepository.save(grossesse);
        return versDtoCalcule(enregistree);
    }

    @PreAuthorize("hasAnyAuthority('FEMME_ENCEINTE', 'CONJOINT')")
    public GrossesseDto obtenirGrossesseActive(String emailFemme) {
        Utilisateur femme = trouverUtilisateurParEmail(emailFemme);
        Grossesse grossesse = grossesseRepository
                .findByFemmeEnceinteIdAndStatut(femme.getId(), StatutGrossesse.EN_COURS)
                .orElseThrow(() -> new ResourceNotFoundException("Aucune grossesse en cours trouvée"));
        return versDtoCalcule(grossesse);
    }

    @PreAuthorize("hasAnyAuthority('FEMME_ENCEINTE', 'CONJOINT')")
    public List<GrossesseDto> obtenirHistorique(String emailFemme) {
        Utilisateur femme = trouverUtilisateurParEmail(emailFemme);
        return grossesseRepository.findByFemmeEnceinteIdOrderByDateCreationDesc(femme.getId()).stream()
                .map(this::versDtoCalcule)
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME', 'ADMIN')")
    public GrossesseDto obtenirParId(Long id) {
        Grossesse grossesse = trouverParId(id);
        return versDtoCalcule(grossesse);
    }

    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME', 'ADMIN')")
    public List<GrossesseDto> listerActives() {
        return grossesseRepository.findByStatut(StatutGrossesse.EN_COURS).stream()
                .map(this::versDtoCalcule)
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME', 'ADMIN')")
    public List<GrossesseDto> listerARisque() {
        return grossesseRepository.findByGrossesseARisqueTrue().stream()
                .map(this::versDtoCalcule)
                .toList();
    }

    @PreAuthorize("hasAuthority('MEDECIN')")
    public GrossesseDto declarerARisque(Long id, DeclarerRisqueRequest request) {
        Grossesse grossesse = trouverParId(id);
        grossesse.setGrossesseARisque(true);
        grossesse.setMotifRisque(request.getMotif());
        return versDtoCalcule(grossesseRepository.save(grossesse));
    }

    @PreAuthorize("hasAuthority('MEDECIN')")
    public GrossesseDto retirerRisque(Long id) {
        Grossesse grossesse = trouverParId(id);
        grossesse.setGrossesseARisque(false);
        grossesse.setMotifRisque(null);
        return versDtoCalcule(grossesseRepository.save(grossesse));
    }

    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME')")
    public GrossesseDto terminer(Long id, TerminerGrossesseRequest request) {
        Grossesse grossesse = trouverParId(id);

        if (!StatutGrossesse.EN_COURS.equals(grossesse.getStatut())) {
            throw new OperationNonAutoriseeException("Cette grossesse est déjà clôturée");
        }

        grossesse.setStatut(request.getStatut());

        return versDtoCalcule(grossesseRepository.save(grossesse));
    }

    // --- Méthodes internes utilisées aussi par CalendrierHelper / SuiviHelper ---

    Grossesse trouverActiveParEmail(String email) {
        Utilisateur femme = trouverUtilisateurParEmail(email);
        return grossesseRepository.findByFemmeEnceinteIdAndStatut(femme.getId(), StatutGrossesse.EN_COURS)
                .orElseThrow(() -> new ResourceNotFoundException("Aucune grossesse en cours trouvée"));
    }

    Grossesse trouverParId(Long id) {
        return grossesseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grossesse introuvable avec l'id : " + id));
    }

    GrossesseDto versDtoCalcule(Grossesse grossesse) {
        int ageJours = calcul.calculerAgeGestationnelJours(grossesse.getDateDernieresRegles());
        int semaine = calcul.calculerSemaineActuelle(grossesse.getDateDernieresRegles());
        String trimestre = calcul.calculerTrimestre(semaine);
        int joursRestants = calcul.calculerJoursRestants(grossesse.getDatePrevueAccouchement());
        return grossesseMapper.versDto(grossesse, ageJours, semaine, trimestre, joursRestants);
    }

    private Utilisateur trouverUtilisateurParEmail(String email) {
        return dossierFamilialHelper.trouverFemmeAssociee(email);
    }
}
