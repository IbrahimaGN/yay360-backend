package sn.yay360.api.service.impl;

import sn.yay360.api.dto.DeclarerGrossesseRequest;
import sn.yay360.api.dto.DeclarerRisqueRequest;
import sn.yay360.api.dto.GrossesseDto;
import sn.yay360.api.dto.TerminerGrossesseRequest;
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
    public GrossesseDto obtenirParId(Long id) {
        return grossesseHelper.obtenirParId(id);
    }

    @Override
    public List<GrossesseDto> listerActives() {
        return grossesseHelper.listerActives();
    }

    @Override
    public List<GrossesseDto> listerARisque() {
        return grossesseHelper.listerARisque();
    }

    @Override
    public GrossesseDto declarerARisque(Long id, DeclarerRisqueRequest request) {
        return grossesseHelper.declarerARisque(id, request);
    }

    @Override
    public GrossesseDto retirerRisque(Long id) {
        return grossesseHelper.retirerRisque(id);
    }

    @Override
    public GrossesseDto terminer(Long id, TerminerGrossesseRequest request) {
        return grossesseHelper.terminer(id, request);
    }
}
