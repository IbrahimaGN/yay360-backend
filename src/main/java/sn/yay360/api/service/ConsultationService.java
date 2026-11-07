package sn.yay360.api.service;

import sn.yay360.api.dto.*;

import java.util.List;

public interface ConsultationService {

    ConsultationDto enregistrer(String emailProfessionnel, EnregistrerConsultationRequest request);

    List<ConsultationDto> mesConsultations(String emailFemme);

    List<ConsultationDto> parGrossesse(String email, Long grossesseId);

    List<ConsultationDto> mesConsultationsProfessionnel(String emailProfessionnel);

    ConsultationDto obtenirParId(String email, Long id);
}
