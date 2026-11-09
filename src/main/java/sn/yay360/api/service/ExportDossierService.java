package sn.yay360.api.service;

/** Export PDF du dossier de grossesse. */
public interface ExportDossierService {

    byte[] exporterMaGrossesse(String emailPatiente);

    byte[] exporterParId(String emailSoignant, Long grossesseId);
}
