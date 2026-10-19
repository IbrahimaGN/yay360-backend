package sn.yay360.api.service;

import sn.yay360.api.dto.DeclarerGrossesseRequest;
import sn.yay360.api.dto.DeclarerRisqueRequest;
import sn.yay360.api.dto.GrossesseDto;
import sn.yay360.api.dto.TerminerGrossesseRequest;

import java.util.List;

public interface GrossesseService {

    GrossesseDto declarer(String emailFemme, DeclarerGrossesseRequest request);

    GrossesseDto obtenirGrossesseActive(String emailFemme);

    List<GrossesseDto> obtenirHistorique(String emailFemme);

    GrossesseDto obtenirParId(Long id);

    List<GrossesseDto> listerActives();

    List<GrossesseDto> listerARisque();

    GrossesseDto declarerARisque(Long id, DeclarerRisqueRequest request);

    GrossesseDto retirerRisque(Long id);

    GrossesseDto terminer(Long id, TerminerGrossesseRequest request);
}
