package sn.yay360.api.service.impl;

import sn.yay360.api.dto.*;
import sn.yay360.api.helper.AlerteHelper;
import sn.yay360.api.service.AlerteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlerteServiceImpl implements AlerteService {

    private final AlerteHelper alerteHelper;

    @Override
    public AlerteDto declarerSymptome(String emailFemme, DeclarerSymptomeRequest request) {
        return alerteHelper.declarerSymptome(emailFemme, request);
    }

    @Override
    public List<AlerteDto> mesAlertes(String emailFemme) {
        return alerteHelper.mesAlertes(emailFemme);
    }

    @Override
    public List<AlerteDto> listerParStatut(String email, String statut) {
        return alerteHelper.listerParStatut(email, statut);
    }

    @Override
    public List<AlerteDto> listerUrgentes(String email) {
        return alerteHelper.listerUrgentes(email);
    }

    @Override
    public AlerteDto prendreEnCharge(String emailProfessionnel, Long id) {
        return alerteHelper.prendreEnCharge(emailProfessionnel, id);
    }

    @Override
    public AlerteDto resoudre(String emailProfessionnel, Long id, PriseEnChargeRequest request) {
        return alerteHelper.resoudre(emailProfessionnel, id, request);
    }

    @Override
    public AlerteDto obtenirParId(String email, Long id) {
        return alerteHelper.obtenirParId(email, id);
    }
}
