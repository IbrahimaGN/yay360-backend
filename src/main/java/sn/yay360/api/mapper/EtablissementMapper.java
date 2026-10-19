package sn.yay360.api.mapper;

import sn.yay360.api.dto.EtablissementDto;
import sn.yay360.api.model.EtablissementSante;
import sn.yay360.api.model.Utilisateur;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EtablissementMapper {

    public EtablissementDto versDto(EtablissementSante etablissement) {
        if (etablissement == null) {
            return null;
        }
        List<String> professionnels = etablissement.getProfessionnels().stream()
                .map(p -> p.getPrenom() + " " + p.getNom())
                .toList();

        return new EtablissementDto(
                etablissement.getId(),
                etablissement.getNom(),
                etablissement.getType(),
                etablissement.getAdresse(),
                etablissement.getVille(),
                etablissement.getTelephone(),
                etablissement.getEmail(),
                etablissement.getLatitude(),
                etablissement.getLongitude(),
                etablissement.getActif(),
                professionnels
        );
    }
}
