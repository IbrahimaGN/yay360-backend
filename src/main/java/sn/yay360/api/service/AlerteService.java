package sn.yay360.api.service;

import sn.yay360.api.dto.AlerteDto;
import sn.yay360.api.dto.DeclarerSymptomeRequest;
import sn.yay360.api.dto.PriseEnChargeRequest;

import java.util.List;

public interface AlerteService {

    AlerteDto declarerSymptome(String emailFemme, DeclarerSymptomeRequest request);

    List<AlerteDto> mesAlertes(String emailFemme);

    List<AlerteDto> listerParStatut(String statut);

    List<AlerteDto> listerUrgentes();

    AlerteDto prendreEnCharge(String emailProfessionnel, Long id);

    AlerteDto resoudre(Long id, PriseEnChargeRequest request);

    AlerteDto obtenirParId(String email, Long id);
}
