package sn.yay360.api.service.impl;

import sn.yay360.api.dto.AffecterProfessionnelRequest;
import sn.yay360.api.dto.EtablissementDto;
import sn.yay360.api.dto.EtablissementRequest;
import sn.yay360.api.helper.EtablissementHelper;
import sn.yay360.api.service.EtablissementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EtablissementServiceImpl implements EtablissementService {

    private final EtablissementHelper etablissementHelper;

    @Override
    public List<EtablissementDto> listerActifs() {
        return etablissementHelper.listerActifs();
    }

    @Override
    public List<EtablissementDto> listerParVille(String ville) {
        return etablissementHelper.listerParVille(ville);
    }

    @Override
    public EtablissementDto obtenirParId(Long id) {
        return etablissementHelper.obtenirParId(id);
    }

    @Override
    public List<EtablissementDto> listerTous() {
        return etablissementHelper.listerTous();
    }

    @Override
    public List<EtablissementDto> mesEtablissements(String emailProfessionnel) {
        return etablissementHelper.mesEtablissements(emailProfessionnel);
    }

    @Override
    public EtablissementDto monEtablissement(String emailAdmin) {
        return etablissementHelper.monEtablissement(emailAdmin);
    }

    @Override
    public EtablissementDto creer(EtablissementRequest request) {
        return etablissementHelper.creer(request);
    }

    @Override
    public EtablissementDto mettreAJour(String emailAuteur, Long id, EtablissementRequest request) {
        return etablissementHelper.mettreAJour(emailAuteur, id, request);
    }

    @Override
    public EtablissementDto affecterProfessionnel(String emailAuteur, Long id, AffecterProfessionnelRequest request) {
        return etablissementHelper.affecterProfessionnel(emailAuteur, id, request);
    }

    @Override
    public EtablissementDto retirerProfessionnel(String emailAuteur, Long id, Long professionnelId) {
        return etablissementHelper.retirerProfessionnel(emailAuteur, id, professionnelId);
    }

    @Override
    public void desactiver(Long id) {
        etablissementHelper.desactiver(id);
    }

    @Override
    public void supprimer(Long id) {
        etablissementHelper.supprimer(id);
    }
}
