package sn.yay360.api.mapper;

import sn.yay360.api.dto.GrossesseDto;
import sn.yay360.api.helper.CalculObstetricalHelper;
import sn.yay360.api.model.Grossesse;
import org.springframework.stereotype.Component;

@Component
public class GrossesseMapper {

    public GrossesseDto versDto(Grossesse grossesse, int ageGestationnelJours, int semaineActuelle,
                                 String trimestre, int joursRestants) {
        GrossesseDto dto = new GrossesseDto();
        dto.setId(grossesse.getId());
        dto.setFemmeEnceinteId(grossesse.getFemmeEnceinte().getId());
        dto.setFemmeEnceinteNomComplet(grossesse.getFemmeEnceinte().getPrenom() + " " + grossesse.getFemmeEnceinte().getNom());
        dto.setDateDernieresRegles(grossesse.getDateDernieresRegles());
        dto.setDatePrevueAccouchement(grossesse.getDatePrevueAccouchement());
        dto.setDebutPeriodeTerme(CalculObstetricalHelper.debutPeriodeTerme(grossesse.getDateDernieresRegles()));
        dto.setFinPeriodeTerme(CalculObstetricalHelper.finPeriodeTerme(grossesse.getDateDernieresRegles()));
        dto.setAgeGestationnelJours(ageGestationnelJours);
        dto.setSemaineActuelle(semaineActuelle);
        dto.setTrimestre(trimestre);
        dto.setJoursRestants(joursRestants);
        dto.setStatut(grossesse.getStatut());
        dto.setGrossesseARisque(grossesse.getGrossesseARisque());
        dto.setMotifRisque(grossesse.getMotifRisque());
        dto.setNombreGrossessesPrecedentes(grossesse.getNombreGrossessesPrecedentes());
        dto.setAntecedents(grossesse.getAntecedents());
        if (grossesse.getEtablissementSuivi() != null) {
            dto.setEtablissementSuiviId(grossesse.getEtablissementSuivi().getId());
            dto.setEtablissementSuiviNom(grossesse.getEtablissementSuivi().getNom());
        }
        dto.setDateFin(grossesse.getDateFin());
        dto.setIssue(grossesse.getIssue());
        dto.setDateCreation(grossesse.getDateCreation());
        return dto;
    }
}
