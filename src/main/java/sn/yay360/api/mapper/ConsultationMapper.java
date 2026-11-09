package sn.yay360.api.mapper;

import sn.yay360.api.dto.ConsultationDto;
import sn.yay360.api.model.Consultation;
import org.springframework.stereotype.Component;

@Component
public class ConsultationMapper {

    public ConsultationDto versDto(Consultation consultation) {
        if (consultation == null) {
            return null;
        }
        return new ConsultationDto(
                consultation.getId(),
                consultation.getGrossesse().getId(),
                consultation.getFemmeEnceinte().getId(),
                consultation.getFemmeEnceinte().getPrenom() + " " + consultation.getFemmeEnceinte().getNom(),
                consultation.getProfessionnel().getId(),
                consultation.getProfessionnel().getPrenom() + " " + consultation.getProfessionnel().getNom(),
                consultation.getRendezVous() != null ? consultation.getRendezVous().getId() : null,
                consultation.getEtablissement() != null ? consultation.getEtablissement().getNom() : null,
                consultation.getDateConsultation(),
                consultation.getPoidsKg(),
                consultation.getTensionArterielle(),
                consultation.getTemperatureC(),
                consultation.getHauteurUterineCm(),
                consultation.getBattementsCoeurFoetal(),
                consultation.getFrequenceCardiaqueMaternelle(),
                consultation.getProteinurie(),
                consultation.getGlycosurie(),
                consultation.getObservations(),
                consultation.getDiagnostic(),
                consultation.getTraitement(),
                consultation.getPrescriptions(),
                consultation.getDateCreation(),
                consultation.getSignauxCliniques() == null || consultation.getSignauxCliniques().isBlank()
                        ? java.util.List.of()
                        : java.util.Arrays.stream(consultation.getSignauxCliniques().split("\n"))
                                .map(l -> l.substring(l.lastIndexOf('|') + 1)).toList()
        );
    }
}
