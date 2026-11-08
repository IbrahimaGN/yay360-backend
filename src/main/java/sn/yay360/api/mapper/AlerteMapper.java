package sn.yay360.api.mapper;

import sn.yay360.api.dto.AlerteDto;
import sn.yay360.api.model.AlerteMedicale;
import org.springframework.stereotype.Component;

@Component
public class AlerteMapper {

    public AlerteDto versDto(AlerteMedicale alerte, String recommandation) {
        if (alerte == null) {
            return null;
        }
        AlerteDto dto = new AlerteDto();
        dto.setId(alerte.getId());
        dto.setGrossesseId(alerte.getGrossesse().getId());
        dto.setFemmeEnceinteId(alerte.getFemmeEnceinte().getId());
        dto.setFemmeEnceinteNomComplet(alerte.getFemmeEnceinte().getPrenom() + " " + alerte.getFemmeEnceinte().getNom());
        dto.setFemmeEnceinteTelephone(alerte.getFemmeEnceinte().getTelephone());
        dto.setTypeSymptome(alerte.getTypeSymptome());
        dto.setDescription(alerte.getDescription());
        dto.setNiveauGravite(alerte.getNiveauGravite());
        dto.setStatut(alerte.getStatut());
        dto.setProfessionnelNomComplet(alerte.getProfessionnelPriseEnCharge() != null
                ? alerte.getProfessionnelPriseEnCharge().getPrenom() + " " + alerte.getProfessionnelPriseEnCharge().getNom()
                : null);
        dto.setResoluParNomComplet(alerte.getProfessionnelResolution() != null
                ? alerte.getProfessionnelResolution().getPrenom() + " " + alerte.getProfessionnelResolution().getNom()
                : null);
        dto.setNotesPriseEnCharge(alerte.getNotesPriseEnCharge());
        dto.setDateDeclaration(alerte.getDateDeclaration());
        dto.setDateResolution(alerte.getDateResolution());
        dto.setRecommandation(recommandation);
        dto.setOrigine(alerte.getOrigine());
        dto.setJustificationTriage(alerte.getJustificationTriage() == null ? java.util.List.of()
                : java.util.List.of(alerte.getJustificationTriage().split("\n")));
        return dto;
    }
}
