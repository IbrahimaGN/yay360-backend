package sn.yay360.api.service;

import sn.yay360.api.dto.EtapeGrossesseDto;
import sn.yay360.api.dto.EtapeGrossesseRequest;

import java.util.List;

public interface EtapeGrossesseService {

    List<EtapeGrossesseDto> listerToutes();

    EtapeGrossesseDto obtenirParSemaine(Integer semaine);

    EtapeGrossesseDto creer(EtapeGrossesseRequest request);

    EtapeGrossesseDto mettreAJour(Long id, EtapeGrossesseRequest request);

    void supprimer(Long id);
}
