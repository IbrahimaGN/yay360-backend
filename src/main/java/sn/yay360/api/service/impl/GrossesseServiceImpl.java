package sn.yay360.api.service.impl;

import sn.yay360.api.dto.*;
import sn.yay360.api.helper.GrossesseHelper;
import sn.yay360.api.service.GrossesseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GrossesseServiceImpl implements GrossesseService {

    private final GrossesseHelper grossesseHelper;

    @Override
    public GrossesseDto declarer(String emailFemme, DeclarerGrossesseRequest request) {
        return grossesseHelper.declarer(emailFemme, request);
    }

    @Override
    public GrossesseDto obtenirGrossesseActive(String emailFemme) {
        return grossesseHelper.obtenirGrossesseActive(emailFemme);
    }

    @Override
    public List<GrossesseDto> obtenirHistorique(String emailFemme) {
        return grossesseHelper.obtenirHistorique(emailFemme);
    }

    @Override
    public GrossesseDto choisirEtablissement(String emailFemme, ChoisirEtablissementRequest request) {
        return grossesseHelper.choisirEtablissement(emailFemme, request);
    }

    @Override
    public GrossesseDto obtenirParId(String email, Long id) {
        return grossesseHelper.obtenirParId(email, id);
    }

    @Override
    public List<GrossesseDto> listerActives(String email) {
        return grossesseHelper.listerActives(email);
    }

    @Override
    public List<GrossesseDto> listerARisque(String email) {
        return grossesseHelper.listerARisque(email);
    }

    @Override
    public List<PatienteRechercheDto> rechercherPatiente(String emailProfessionnel, String telephone, String email) {
        return grossesseHelper.rechercherPatiente(emailProfessionnel, telephone, email);
    }

    @Override
    public GrossesseDto rattacher(String emailProfessionnel, Long id, RattacherGrossesseRequest request) {
        return grossesseHelper.rattacher(emailProfessionnel, id, request);
    }

    @Override
    public GrossesseDto declarerARisque(String emailMedecin, Long id, DeclarerRisqueRequest request) {
        return grossesseHelper.declarerARisque(emailMedecin, id, request);
    }

    @Override
    public GrossesseDto retirerRisque(String emailMedecin, Long id) {
        return grossesseHelper.retirerRisque(emailMedecin, id);
    }

    @Override
    public GrossesseDto terminer(String emailMedecin, Long id, TerminerGrossesseRequest request) {
        return grossesseHelper.terminer(emailMedecin, id, request);
    }
}
