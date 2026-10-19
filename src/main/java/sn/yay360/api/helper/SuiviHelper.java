package sn.yay360.api.helper;

import sn.yay360.api.dto.ConseilDto;
import sn.yay360.api.dto.SuiviHebdomadaireDto;
import sn.yay360.api.mapper.ConseilMapper;
import sn.yay360.api.model.EtapeGrossesse;
import sn.yay360.api.model.Grossesse;
import sn.yay360.api.repo.ConseilGrossesseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Génère l'accompagnement personnalisé hebdomadaire (§5.4) : combine la semaine réelle
 * de la grossesse avec le contenu de référence et les conseils actifs correspondants.
 */
@Component
@RequiredArgsConstructor
public class SuiviHelper {

    private final GrossesseHelper grossesseHelper;
    private final EtapeGrossesseHelper etapeGrossesseHelper;
    private final ConseilGrossesseRepository conseilRepository;
    private final ConseilMapper conseilMapper;
    private final CalculObstetricalHelper calcul;

    @PreAuthorize("hasAnyAuthority('FEMME_ENCEINTE', 'CONJOINT')")
    public SuiviHebdomadaireDto obtenirMonSuivi(String email) {
        Grossesse grossesse = grossesseHelper.trouverActiveParEmail(email);
        return genererSuivi(grossesse);
    }

    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME', 'ADMIN')")
    public SuiviHebdomadaireDto obtenirSuiviParId(Long grossesseId) {
        Grossesse grossesse = grossesseHelper.trouverParId(grossesseId);
        return genererSuivi(grossesse);
    }

    private SuiviHebdomadaireDto genererSuivi(Grossesse grossesse) {
        int ageJours = calcul.calculerAgeGestationnelJours(grossesse.getDateDernieresRegles());
        int semaine = calcul.calculerSemaineActuelle(grossesse.getDateDernieresRegles());
        String trimestre = calcul.calculerTrimestre(semaine);
        int joursRestants = calcul.calculerJoursRestants(grossesse.getDatePrevueAccouchement());

        Optional<EtapeGrossesse> etapeOpt;
        try {
            etapeOpt = Optional.of(etapeGrossesseHelper.trouverParSemaine(semaine));
        } catch (Exception ex) {
            etapeOpt = Optional.empty();
        }

        List<ConseilDto> conseils = conseilRepository
                .findByActifTrueAndSemaineDebutLessThanEqualAndSemaineFinGreaterThanEqual(semaine, semaine).stream()
                .map(conseilMapper::versDto)
                .toList();

        SuiviHebdomadaireDto dto = new SuiviHebdomadaireDto();
        dto.setGrossesseId(grossesse.getId());
        dto.setSemaineActuelle(semaine);
        dto.setTrimestre(trimestre);
        dto.setAgeGestationnelJours(ageJours);
        dto.setDatePrevueAccouchement(grossesse.getDatePrevueAccouchement());
        dto.setJoursRestants(joursRestants);
        dto.setGrossesseARisque(grossesse.getGrossesseARisque());

        etapeOpt.ifPresent(etape -> {
            dto.setDeveloppementBebe(etape.getDeveloppementBebe());
            dto.setChangementsMere(etape.getChangementsMere());
            dto.setObjectifsSante(etape.getObjectifsSante());
            dto.setConseilsAlimentation(etape.getConseilsAlimentation());
            dto.setConseilsHydratation(etape.getConseilsHydratation());
            dto.setExercicesPhysiques(etape.getExercicesPhysiques());
            dto.setConseilsSommeil(etape.getConseilsSommeil());
            dto.setPrecautions(etape.getPrecautions());
            dto.setExamensRecommandes(etape.getExamensRecommandes());
            dto.setSignesAlerte(etape.getSignesAlerte());
        });

        dto.setConseilsPersonnalises(conseils);
        return dto;
    }
}
