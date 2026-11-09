package sn.yay360.api.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sn.yay360.api.helper.ExportDossierHelper;
import sn.yay360.api.service.ExportDossierService;

@Service
@RequiredArgsConstructor
public class ExportDossierServiceImpl implements ExportDossierService {

    private final ExportDossierHelper exportDossierHelper;

    @Override
    public byte[] exporterMaGrossesse(String emailPatiente) {
        return exportDossierHelper.exporterMaGrossesse(emailPatiente);
    }

    @Override
    public byte[] exporterParId(String emailSoignant, Long grossesseId) {
        return exportDossierHelper.exporterParId(emailSoignant, grossesseId);
    }
}
