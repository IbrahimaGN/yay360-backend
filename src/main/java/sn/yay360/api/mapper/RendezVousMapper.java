package sn.yay360.api.mapper;

import sn.yay360.api.dto.RendezVousDto;
import sn.yay360.api.model.RendezVous;
import org.springframework.stereotype.Component;

@Component
public class RendezVousMapper {

    public RendezVousDto versDto(RendezVous rdv) {
        if (rdv == null) {
            return null;
        }
        return new RendezVousDto(
                rdv.getId(),
                rdv.getGrossesse().getId(),
                rdv.getFemmeEnceinte().getId(),
                rdv.getFemmeEnceinte().getPrenom() + " " + rdv.getFemmeEnceinte().getNom(),
                rdv.getProfessionnel() != null ? rdv.getProfessionnel().getId() : null,
                rdv.getProfessionnel() != null ? rdv.getProfessionnel().getPrenom() + " " + rdv.getProfessionnel().getNom() : null,
                rdv.getEtablissement() != null ? rdv.getEtablissement().getId() : null,
                rdv.getEtablissement() != null ? rdv.getEtablissement().getNom() : null,
                rdv.getDateHeure(),
                rdv.getType(),
                rdv.getStatut(),
                rdv.getMotif(),
                rdv.getNotesAnnulation(),
                rdv.getDateCreation()
        );
    }
}
