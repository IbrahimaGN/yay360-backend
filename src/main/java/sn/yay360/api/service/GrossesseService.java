package sn.yay360.api.service;

import sn.yay360.api.dto.*;

import java.util.List;

public interface GrossesseService {

    GrossesseDto declarer(String emailFemme, DeclarerGrossesseRequest request);

    GrossesseDto obtenirGrossesseActive(String emailFemme);

    List<GrossesseDto> obtenirHistorique(String emailFemme);

    GrossesseDto choisirEtablissement(String emailFemme, ChoisirEtablissementRequest request);

    GrossesseDto obtenirParId(String email, Long id);

    List<GrossesseDto> listerActives(String email);

    List<GrossesseDto> listerARisque(String email);

    List<PatienteRechercheDto> rechercherPatiente(String emailProfessionnel, String telephone, String email);

    GrossesseDto rattacher(String emailProfessionnel, Long id, RattacherGrossesseRequest request);

    GrossesseDto declarerARisque(String emailMedecin, Long id, DeclarerRisqueRequest request);

    GrossesseDto retirerRisque(String emailMedecin, Long id);

    GrossesseDto terminer(String emailMedecin, Long id, TerminerGrossesseRequest request);
}
