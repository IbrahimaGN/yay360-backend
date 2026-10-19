package sn.yay360.api.mapper;

import sn.yay360.api.dto.ConseilDto;
import sn.yay360.api.model.ConseilGrossesse;
import org.springframework.stereotype.Component;

@Component
public class ConseilMapper {

    public ConseilDto versDto(ConseilGrossesse conseil) {
        if (conseil == null) {
            return null;
        }
        String auteur = conseil.getAuteur() != null
                ? conseil.getAuteur().getPrenom() + " " + conseil.getAuteur().getNom()
                : null;

        return new ConseilDto(
                conseil.getId(),
                conseil.getSemaineDebut(),
                conseil.getSemaineFin(),
                conseil.getTrimestre(),
                conseil.getCategorie(),
                conseil.getTitre(),
                conseil.getContenu(),
                conseil.getActif(),
                auteur,
                conseil.getDateCreation()
        );
    }
}
