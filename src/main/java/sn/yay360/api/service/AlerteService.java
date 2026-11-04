package sn.yay360.api.service;

import sn.yay360.api.dto.*;

import java.util.List;

public interface AlerteService {

    AlerteDto declarerSymptome(String emailFemme, DeclarerSymptomeRequest request);

    List<AlerteDto> mesAlertes(String emailFemme);

    List<AlerteDto> listerParStatut(String email, String statut);

    List<AlerteDto> listerUrgentes(String email);

    AlerteDto prendreEnCharge(String emailProfessionnel, Long id);

    AlerteDto resoudre(String emailProfessionnel, Long id, PriseEnChargeRequest request);

    AlerteDto obtenirParId(String email, Long id);
}
