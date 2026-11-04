package sn.yay360.api.service.impl;

import sn.yay360.api.dto.*;
import sn.yay360.api.helper.RendezVousHelper;
import sn.yay360.api.service.RendezVousService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RendezVousServiceImpl implements RendezVousService {

    private final RendezVousHelper rendezVousHelper;

    @Override
    public RendezVousDto prendre(String emailFemme, PrendreRendezVousRequest request) {
        return rendezVousHelper.prendre(emailFemme, request);
    }

    @Override
    public RendezVousDto planifier(String emailProfessionnel, PlanifierRendezVousRequest request) {
        return rendezVousHelper.planifier(emailProfessionnel, request);
    }

    @Override
    public List<RendezVousDto> mesRendezVous(String emailFemme) {
        return rendezVousHelper.mesRendezVous(emailFemme);
    }

    @Override
    public List<RendezVousDto> mesRendezVousAVenir(String emailFemme) {
        return rendezVousHelper.mesRendezVousAVenir(emailFemme);
    }

    @Override
    public List<RendezVousDto> rendezVousDuProfessionnel(String emailProfessionnel) {
        return rendezVousHelper.rendezVousDuProfessionnel(emailProfessionnel);
    }

    @Override
    public List<RendezVousDto> listerParStatut(String email, String statut) {
        return rendezVousHelper.listerParStatut(email, statut);
    }

    @Override
    public List<RendezVousDto> listerParGrossesse(String email, Long grossesseId) {
        return rendezVousHelper.listerParGrossesse(email, grossesseId);
    }

    @Override
    public RendezVousDto obtenirParId(String email, Long id) {
        return rendezVousHelper.obtenirParId(email, id);
    }

    @Override
    public RendezVousDto modifier(String email, Long id, ModifierRendezVousRequest request) {
        return rendezVousHelper.modifier(email, id, request);
    }

    @Override
    public RendezVousDto annuler(String email, Long id, AnnulerRendezVousRequest request) {
        return rendezVousHelper.annuler(email, id, request);
    }

    @Override
    public RendezVousDto changerStatut(String emailProfessionnel, Long id, ChangerStatutRendezVousRequest request) {
        return rendezVousHelper.changerStatut(emailProfessionnel, id, request);
    }
}
