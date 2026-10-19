package sn.yay360.api.service.impl;

import sn.yay360.api.dto.ConsultationDto;
import sn.yay360.api.dto.EnregistrerConsultationRequest;
import sn.yay360.api.helper.ConsultationHelper;
import sn.yay360.api.service.ConsultationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultationServiceImpl implements ConsultationService {

    private final ConsultationHelper consultationHelper;

    @Override
    public ConsultationDto enregistrer(String emailProfessionnel, EnregistrerConsultationRequest request) {
        return consultationHelper.enregistrer(emailProfessionnel, request);
    }

    @Override
    public List<ConsultationDto> mesConsultations(String emailFemme) {
        return consultationHelper.mesConsultations(emailFemme);
    }

    @Override
    public List<ConsultationDto> parGrossesse(Long grossesseId) {
        return consultationHelper.parGrossesse(grossesseId);
    }

    @Override
    public List<ConsultationDto> mesConsultationsProfessionnel(String emailProfessionnel) {
        return consultationHelper.mesConsultationsProfessionnel(emailProfessionnel);
    }

    @Override
    public ConsultationDto obtenirParId(String email, Long id) {
        return consultationHelper.obtenirParId(email, id);
    }
}
