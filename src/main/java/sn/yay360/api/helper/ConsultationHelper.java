package sn.yay360.api.helper;

import sn.yay360.api.dto.ConsultationDto;
import sn.yay360.api.dto.EnregistrerConsultationRequest;
import sn.yay360.api.exception.OperationNonAutoriseeException;
import sn.yay360.api.exception.ResourceNotFoundException;
import sn.yay360.api.mapper.ConsultationMapper;
import sn.yay360.api.model.*;
import sn.yay360.api.repo.ConsultationRepository;
import sn.yay360.api.repo.EtablissementSanteRepository;
import sn.yay360.api.repo.RendezVousRepository;
import sn.yay360.api.repo.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Contient toute la logique métier liée à l'enregistrement des consultations (§5.6).
 */
@Component
@RequiredArgsConstructor
public class ConsultationHelper {

    private final ConsultationRepository consultationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EtablissementSanteRepository etablissementRepository;
    private final RendezVousRepository rendezVousRepository;
    private final GrossesseHelper grossesseHelper;
    private final ConsultationMapper consultationMapper;
    private final DossierFamilialHelper dossierFamilialHelper;

    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME')")
    public ConsultationDto enregistrer(String emailProfessionnel, EnregistrerConsultationRequest request) {
        Utilisateur professionnel = utilisateurRepository.findByEmail(emailProfessionnel)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + emailProfessionnel));

        Grossesse grossesse = grossesseHelper.trouverParId(request.getGrossesseId());

        Consultation consultation = new Consultation();
        consultation.setGrossesse(grossesse);
        consultation.setFemmeEnceinte(grossesse.getFemmeEnceinte());
        consultation.setProfessionnel(professionnel);
        consultation.setPoidsKg(request.getPoidsKg());
        consultation.setTensionArterielle(request.getTensionArterielle());
        consultation.setTemperatureC(request.getTemperatureC());
        consultation.setHauteurUterineCm(request.getHauteurUterineCm());
        consultation.setBattementsCoeurFoetal(request.getBattementsCoeurFoetal());
        consultation.setObservations(request.getObservations());
        consultation.setDiagnostic(request.getDiagnostic());
        consultation.setTraitement(request.getTraitement());
        consultation.setPrescriptions(request.getPrescriptions());

        if (request.getEtablissementId() != null) {
            EtablissementSante etablissement = etablissementRepository.findById(request.getEtablissementId())
                    .orElseThrow(() -> new ResourceNotFoundException("Établissement introuvable avec l'id : " + request.getEtablissementId()));
            consultation.setEtablissement(etablissement);
        }

        if (request.getRendezVousId() != null) {
            RendezVous rdv = rendezVousRepository.findById(request.getRendezVousId())
                    .orElseThrow(() -> new ResourceNotFoundException("Rendez-vous introuvable avec l'id : " + request.getRendezVousId()));
            consultation.setRendezVous(rdv);
            // Principe de compensation : le rendez-vous honoré passe automatiquement à TERMINE
            rdv.setStatut(StatutRendezVous.TERMINE);
            rendezVousRepository.save(rdv);
        }

        return consultationMapper.versDto(consultationRepository.save(consultation));
    }

    @PreAuthorize("hasAnyAuthority('FEMME_ENCEINTE', 'CONJOINT')")
    public List<ConsultationDto> mesConsultations(String emailFemme) {
        Utilisateur femme = dossierFamilialHelper.trouverFemmeAssociee(emailFemme);
        return consultationRepository.findByFemmeEnceinteIdOrderByDateConsultationDesc(femme.getId()).stream()
                .map(consultationMapper::versDto)
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME', 'ADMIN')")
    public List<ConsultationDto> parGrossesse(Long grossesseId) {
        return consultationRepository.findByGrossesseIdOrderByDateConsultationDesc(grossesseId).stream()
                .map(consultationMapper::versDto)
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME')")
    public List<ConsultationDto> mesConsultationsProfessionnel(String emailProfessionnel) {
        Utilisateur professionnel = utilisateurRepository.findByEmail(emailProfessionnel)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + emailProfessionnel));
        return consultationRepository.findByProfessionnelIdOrderByDateConsultationDesc(professionnel.getId()).stream()
                .map(consultationMapper::versDto)
                .toList();
    }

    public ConsultationDto obtenirParId(String email, Long id) {
        Consultation consultation = consultationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation introuvable avec l'id : " + id));

        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + email));

        boolean estProprietaire = dossierFamilialHelper.aAccesAuDossier(utilisateur, consultation.getFemmeEnceinte());
        boolean estPersonnelAutorise = List.of(RoleUtilisateur.ADMIN, RoleUtilisateur.MEDECIN, RoleUtilisateur.SAGE_FEMME)
                .contains(utilisateur.getRole());

        if (!estProprietaire && !estPersonnelAutorise) {
            throw new OperationNonAutoriseeException("Vous n'avez pas accès à cette consultation");
        }

        return consultationMapper.versDto(consultation);
    }
}
