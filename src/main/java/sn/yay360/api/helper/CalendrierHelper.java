package sn.yay360.api.helper;

import sn.yay360.api.dto.CalendrierGrossesseDto;
import sn.yay360.api.dto.CalendrierSemaineDto;
import sn.yay360.api.model.EtapeGrossesse;
import sn.yay360.api.model.Grossesse;
import sn.yay360.api.repo.EtapeGrossesseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Génère le calendrier intelligent de grossesse (§5.5) : pour chaque semaine (1 à 40),
 * combine le contenu de référence administrable avec la progression réelle de la grossesse.
 */
@Component
@RequiredArgsConstructor
public class CalendrierHelper {

    private final GrossesseHelper grossesseHelper;
    private final EtapeGrossesseRepository etapeGrossesseRepository;
    private final CalculObstetricalHelper calcul;

    @PreAuthorize("hasAnyAuthority('FEMME_ENCEINTE', 'CONJOINT')")
    public CalendrierGrossesseDto obtenirMonCalendrier(String email) {
        Grossesse grossesse = grossesseHelper.trouverActiveParEmail(email);
        return genererCalendrier(grossesse);
    }

    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME', 'ADMIN')")
    public CalendrierGrossesseDto obtenirCalendrierParId(Long grossesseId) {
        Grossesse grossesse = grossesseHelper.trouverParId(grossesseId);
        return genererCalendrier(grossesse);
    }

    private CalendrierGrossesseDto genererCalendrier(Grossesse grossesse) {
        int semaineActuelle = calcul.calculerSemaineActuelle(grossesse.getDateDernieresRegles());

        Map<Integer, EtapeGrossesse> etapesParSemaine = new HashMap<>();
        for (EtapeGrossesse etape : etapeGrossesseRepository.findAllByOrderBySemaineAsc()) {
            etapesParSemaine.put(etape.getSemaine(), etape);
        }

        List<CalendrierSemaineDto> semaines = new java.util.ArrayList<>();
        for (int s = 1; s <= CalculObstetricalHelper.DUREE_GROSSESSE_SEMAINES; s++) {
            EtapeGrossesse etape = etapesParSemaine.get(s);
            String statutSemaine = s < semaineActuelle ? "PASSEE" : (s == semaineActuelle ? "ACTUELLE" : "A_VENIR");

            semaines.add(new CalendrierSemaineDto(
                    s,
                    calcul.calculerTrimestre(s),
                    statutSemaine,
                    etape != null ? etape.getDeveloppementBebe() : null,
                    etape != null ? etape.getChangementsMere() : null,
                    etape != null ? etape.getExamensRecommandes() : null,
                    etape != null ? etape.getObjectifsSante() : null
            ));
        }

        return new CalendrierGrossesseDto(
                grossesse.getId(),
                semaineActuelle,
                grossesse.getDatePrevueAccouchement(),
                semaines
        );
    }
}
