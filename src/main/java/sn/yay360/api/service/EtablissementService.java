package sn.yay360.api.service;

import sn.yay360.api.dto.AffecterProfessionnelRequest;
import sn.yay360.api.dto.EtablissementDto;
import sn.yay360.api.dto.EtablissementRequest;

import java.util.List;

public interface EtablissementService {

    List<EtablissementDto> listerActifs();

    List<EtablissementDto> listerParVille(String ville);

    EtablissementDto obtenirParId(Long id);

    List<EtablissementDto> listerTous();

    List<EtablissementDto> mesEtablissements(String emailProfessionnel);

    EtablissementDto creer(EtablissementRequest request);

    EtablissementDto mettreAJour(Long id, EtablissementRequest request);

    EtablissementDto affecterProfessionnel(Long id, AffecterProfessionnelRequest request);

    EtablissementDto retirerProfessionnel(Long id, Long professionnelId);

    void desactiver(Long id);

    void supprimer(Long id);
}
