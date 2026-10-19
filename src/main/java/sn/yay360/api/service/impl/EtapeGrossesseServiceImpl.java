package sn.yay360.api.service.impl;

import sn.yay360.api.dto.EtapeGrossesseDto;
import sn.yay360.api.dto.EtapeGrossesseRequest;
import sn.yay360.api.helper.EtapeGrossesseHelper;
import sn.yay360.api.service.EtapeGrossesseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EtapeGrossesseServiceImpl implements EtapeGrossesseService {

    private final EtapeGrossesseHelper etapeGrossesseHelper;

    @Override
    public List<EtapeGrossesseDto> listerToutes() {
        return etapeGrossesseHelper.listerToutes();
    }

    @Override
    public EtapeGrossesseDto obtenirParSemaine(Integer semaine) {
        return etapeGrossesseHelper.obtenirParSemaine(semaine);
    }

    @Override
    public EtapeGrossesseDto creer(EtapeGrossesseRequest request) {
        return etapeGrossesseHelper.creer(request);
    }

    @Override
    public EtapeGrossesseDto mettreAJour(Long id, EtapeGrossesseRequest request) {
        return etapeGrossesseHelper.mettreAJour(id, request);
    }

    @Override
    public void supprimer(Long id) {
        etapeGrossesseHelper.supprimer(id);
    }
}
