package sn.yay360.api.service.impl;

import sn.yay360.api.dto.CalendrierGrossesseDto;
import sn.yay360.api.helper.CalendrierHelper;
import sn.yay360.api.service.CalendrierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CalendrierServiceImpl implements CalendrierService {

    private final CalendrierHelper calendrierHelper;

    @Override
    public CalendrierGrossesseDto obtenirMonCalendrier(String email) {
        return calendrierHelper.obtenirMonCalendrier(email);
    }

    @Override
    public CalendrierGrossesseDto obtenirCalendrierParId(Long grossesseId) {
        return calendrierHelper.obtenirCalendrierParId(grossesseId);
    }
}
