package sn.yay360.api.helper;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.yay360.api.mapper.AlerteMapper;
import sn.yay360.api.mapper.ConsultationMapper;
import sn.yay360.api.mapper.RendezVousMapper;
import sn.yay360.api.model.Grossesse;
import sn.yay360.api.model.RendezVous;
import sn.yay360.api.model.StatutGrossesse;
import sn.yay360.api.model.Utilisateur;
import sn.yay360.api.repo.AlerteMedicaleRepository;
import sn.yay360.api.repo.ConsultationRepository;
import sn.yay360.api.repo.RendezVousRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Export PDF du dossier de grossesse : remplace le carnet papier égaré (besoin exprimé par la sage-femme).
 *
 * <p>Accès : la patiente (sa grossesse en cours) et les soignants qui la suivent. Ni le conjoint ni
 * l'administrateur : le document contient tout le contenu clinique, y compris l'issue de la grossesse.</p>
 */
@Component
@RequiredArgsConstructor
public class ExportDossierHelper {

    private final GrossesseHelper grossesseHelper;
    private final IntelligenceCliniqueHelper intelligenceCliniqueHelper;
    private final AccesDossierHelper acces;
    private final ConsultationRepository consultationRepository;
    private final AlerteMedicaleRepository alerteRepository;
    private final RendezVousRepository rendezVousRepository;
    private final ConsultationMapper consultationMapper;
    private final AlerteMapper alerteMapper;
    private final RendezVousMapper rendezVousMapper;
    private final DossierPdfRenderer renderer;

    @PreAuthorize("hasAuthority('FEMME_ENCEINTE')")
    @Transactional(readOnly = true)
    public byte[] exporterMaGrossesse(String emailPatiente) {
        return exporter(grossesseHelper.trouverActiveParEmail(emailPatiente));
    }

    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME')")
    @Transactional(readOnly = true)
    public byte[] exporterParId(String emailSoignant, Long grossesseId) {
        Grossesse grossesse = grossesseHelper.trouverParId(grossesseId);
        Utilisateur soignant = acces.utilisateur(emailSoignant);
        if (!acces.suit(soignant, grossesse)) {
            throw new sn.yay360.api.exception.OperationNonAutoriseeException(
                    "Vous ne suivez pas cette patiente : export du dossier refusé");
        }
        return exporter(grossesse);
    }

    private byte[] exporter(Grossesse grossesse) {
        Utilisateur femme = grossesse.getFemmeEnceinte();
        LocalDateTime maintenant = LocalDateTime.now();
        List<RendezVous> aVenir = rendezVousRepository.findByGrossesseIdOrderByDateHeureDesc(grossesse.getId()).stream()
                .filter(r -> RendezVousHelper.STATUTS_ACTIFS.contains(r.getStatut()) && r.getDateHeure().isAfter(maintenant))
                .sorted(Comparator.comparing(RendezVous::getDateHeure))
                .toList();
        return renderer.rendre(new DossierPdfRenderer.Dossier(
                grossesseHelper.versDtoCalcule(grossesse),
                femme.getPrenom() + " " + femme.getNom(),
                femme.getDateNaissance(),
                femme.getTelephone(),
                consultationRepository.findByGrossesseIdOrderByDateConsultationDesc(grossesse.getId()).stream()
                        .map(consultationMapper::versDto).toList(),
                alerteRepository.findByGrossesseIdOrderByDateDeclarationDesc(grossesse.getId()).stream()
                        .map(a -> alerteMapper.versDto(a, null)).toList(),
                aVenir.stream().map(rendezVousMapper::versDto).toList(),
                StatutGrossesse.EN_COURS.equals(grossesse.getStatut()) ? intelligenceCliniqueHelper.planCpn(grossesse) : null,
                maintenant));
    }
}
