package sn.yay360.api.service.impl;

import sn.yay360.api.dto.ConseilDto;
import sn.yay360.api.dto.ConseilRequest;
import sn.yay360.api.helper.ConseilHelper;
import sn.yay360.api.service.ConseilService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConseilServiceImpl implements ConseilService {

    private final ConseilHelper conseilHelper;

    @Override
    public List<ConseilDto> obtenirPourSemaine(Integer semaine) {
        return conseilHelper.obtenirPourSemaine(semaine);
    }

    @Override
    public List<ConseilDto> obtenirParCategorie(String categorie) {
        return conseilHelper.obtenirParCategorie(categorie);
    }

    @Override
    public List<ConseilDto> listerTous() {
        return conseilHelper.listerTous();
    }

    @Override
    public ConseilDto creer(String emailAuteur, ConseilRequest request) {
        return conseilHelper.creer(emailAuteur, request);
    }

    @Override
    public ConseilDto mettreAJour(Long id, ConseilRequest request) {
        return conseilHelper.mettreAJour(id, request);
    }

    @Override
    public ConseilDto basculerActivation(Long id) {
        return conseilHelper.basculerActivation(id);
    }

    @Override
    public void supprimer(Long id) {
        conseilHelper.supprimer(id);
    }
}
