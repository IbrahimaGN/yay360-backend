package sn.yay360.api.service;

import sn.yay360.api.dto.ConseilDto;
import sn.yay360.api.dto.ConseilRequest;

import java.util.List;

public interface ConseilService {

    List<ConseilDto> obtenirPourSemaine(Integer semaine);

    List<ConseilDto> obtenirParCategorie(String categorie);

    List<ConseilDto> listerTous();

    ConseilDto creer(String emailAuteur, ConseilRequest request);

    ConseilDto mettreAJour(Long id, ConseilRequest request);

    ConseilDto basculerActivation(Long id);

    void supprimer(Long id);
}
