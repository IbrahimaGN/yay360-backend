package sn.yay360.api.service.impl;

import sn.yay360.api.dto.*;
import sn.yay360.api.helper.CalendrierHelper;
import sn.yay360.api.service.CalendrierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CalendrierServiceImpl implements CalendrierService {

    private final CalendrierHelper calendrierHelper;

    @Override
    public CalendrierGrossesseDto obtenirMonCalendrier(String email) {
        return calendrierHelper.obtenirMonCalendrier(email);
    }

    @Override
    public CalendrierGrossesseDto obtenirCalendrierParId(String email, Long grossesseId) {
        return calendrierHelper.obtenirCalendrierParId(email, grossesseId);
    }
}
