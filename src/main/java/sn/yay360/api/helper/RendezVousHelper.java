package sn.yay360.api.helper;

import sn.yay360.api.dto.*;
import sn.yay360.api.exception.OperationNonAutoriseeException;
import sn.yay360.api.exception.ResourceNotFoundException;
import sn.yay360.api.mapper.RendezVousMapper;
import sn.yay360.api.model.EtablissementSante;
import sn.yay360.api.model.Grossesse;
import sn.yay360.api.model.RendezVous;
import sn.yay360.api.model.StatutRendezVous;
import sn.yay360.api.model.StatutGrossesse;
import sn.yay360.api.model.Utilisateur;
import sn.yay360.api.repo.EtablissementSanteRepository;
import sn.yay360.api.repo.RendezVousRepository;
import sn.yay360.api.repo.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Contient toute la logique métier liée à la prise et au suivi des rendez-vous (§5.7).
 */
@Component
@RequiredArgsConstructor
public class RendezVousHelper {

    private final RendezVousRepository rendezVousRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EtablissementSanteRepository etablissementRepository;
    private final GrossesseHelper grossesseHelper;
    private final RendezVousMapper rendezVousMapper;
    private final DossierFamilialHelper dossierFamilialHelper;

    @PreAuthorize("hasAnyAuthority('FEMME_ENCEINTE', 'CONJOINT')")
    public RendezVousDto prendre(String emailFemme, PrendreRendezVousRequest request) {
        Grossesse grossesse = request.getGrossesseId() == null
                ? grossesseHelper.trouverActiveParEmail(emailFemme)
                : grossesseHelper.trouverParId(request.getGrossesseId());
        Utilisateur femme = dossierFamilialHelper.trouverFemmeAssociee(emailFemme);
        if (!dossierFamilialHelper.aAccesAuDossier(femme, grossesse.getFemmeEnceinte())) {
            throw new OperationNonAutoriseeException("Vous n'avez pas accès à ce dossier de rendez-vous");
        }
        if (!StatutGrossesse.EN_COURS.equals(grossesse.getStatut())) {
            throw new OperationNonAutoriseeException("Un rendez-vous ne peut être rattaché qu'à une grossesse en cours");
        }

        RendezVous rdv = new RendezVous();
        rdv.setGrossesse(grossesse);
        rdv.setFemmeEnceinte(grossesse.getFemmeEnceinte());
        rdv.setDateHeure(request.getDateHeure());
        rdv.setType(request.getType());
        rdv.setMotif(request.getMotif());
        rdv.setStatut(StatutRendezVous.PLANIFIE);

        if (request.getProfessionnelId() != null) {
            rdv.setProfessionnel(trouverProfessionnel(request.getProfessionnelId()));
        }
        if (request.getEtablissementId() != null) {
            rdv.setEtablissement(trouverEtablissement(request.getEtablissementId()));
        }

        return rendezVousMapper.versDto(rendezVousRepository.save(rdv));
    }

    @PreAuthorize("hasAnyAuthority('FEMME_ENCEINTE', 'CONJOINT')")
    public List<RendezVousDto> mesRendezVous(String emailFemme) {
        Utilisateur femme = dossierFamilialHelper.trouverFemmeAssociee(emailFemme);
        return rendezVousRepository.findByFemmeEnceinteIdOrderByDateHeureDesc(femme.getId()).stream()
                .map(rendezVousMapper::versDto)
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('FEMME_ENCEINTE', 'CONJOINT')")
    public List<RendezVousDto> mesRendezVousAVenir(String emailFemme) {
        Utilisateur femme = dossierFamilialHelper.trouverFemmeAssociee(emailFemme);
        return rendezVousRepository
                .findByFemmeEnceinteIdAndDateHeureAfterOrderByDateHeureAsc(femme.getId(), LocalDateTime.now()).stream()
                .map(rendezVousMapper::versDto)
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME')")
    public List<RendezVousDto> rendezVousDuProfessionnel(String emailProfessionnel) {
        Utilisateur professionnel = trouverUtilisateur(emailProfessionnel);
        return rendezVousRepository.findByProfessionnelIdOrderByDateHeureAsc(professionnel.getId()).stream()
                .map(rendezVousMapper::versDto)
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME', 'ADMIN')")
    public List<RendezVousDto> listerParStatut(String statut) {
        return rendezVousRepository.findByStatutOrderByDateHeureAsc(statut).stream()
                .map(rendezVousMapper::versDto)
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME', 'ADMIN')")
    public List<RendezVousDto> listerParGrossesse(Long grossesseId) {
        return rendezVousRepository.findByGrossesseIdOrderByDateHeureDesc(grossesseId).stream()
                .map(rendezVousMapper::versDto)
                .toList();
    }

    public RendezVousDto obtenirParId(String email, Long id) {
        RendezVous rdv = trouverParId(id);
        verifierAcces(email, rdv);
        return rendezVousMapper.versDto(rdv);
    }

    public RendezVousDto modifier(String email, Long id, ModifierRendezVousRequest request) {
        RendezVous rdv = trouverParId(id);
        verifierAcces(email, rdv);
        verifierModifiable(rdv);

        rdv.setDateHeure(request.getDateHeure());
        if (request.getMotif() != null) {
            rdv.setMotif(request.getMotif());
        }
        return rendezVousMapper.versDto(rendezVousRepository.save(rdv));
    }

    public RendezVousDto annuler(String email, Long id, AnnulerRendezVousRequest request) {
        RendezVous rdv = trouverParId(id);
        verifierAcces(email, rdv);
        verifierModifiable(rdv);

        rdv.setStatut(StatutRendezVous.ANNULE);
        rdv.setNotesAnnulation(request.getNotesAnnulation());
        return rendezVousMapper.versDto(rendezVousRepository.save(rdv));
    }

    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME', 'ADMIN')")
    public RendezVousDto changerStatut(Long id, ChangerStatutRendezVousRequest request) {
        RendezVous rdv = trouverParId(id);
        rdv.setStatut(request.getStatut());
        return rendezVousMapper.versDto(rendezVousRepository.save(rdv));
    }

    // --- internes ---

    RendezVous trouverParId(Long id) {
        return rendezVousRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rendez-vous introuvable avec l'id : " + id));
    }

    private void verifierAcces(String email, RendezVous rdv) {
        Utilisateur utilisateur = trouverUtilisateur(email);
        boolean estProprietaire = dossierFamilialHelper.aAccesAuDossier(utilisateur, rdv.getFemmeEnceinte());
        boolean estProfessionnelAssigne = rdv.getProfessionnel() != null
                && rdv.getProfessionnel().getId().equals(utilisateur.getId());
        boolean estPersonnelAutorise = "ADMIN".equals(utilisateur.getRole())
                || "MEDECIN".equals(utilisateur.getRole())
                || "SAGE_FEMME".equals(utilisateur.getRole());

        if (!estProprietaire && !estProfessionnelAssigne && !estPersonnelAutorise) {
            throw new OperationNonAutoriseeException("Vous n'avez pas accès à ce rendez-vous");
        }
    }

    private void verifierModifiable(RendezVous rdv) {
        if (StatutRendezVous.ANNULE.equals(rdv.getStatut()) || StatutRendezVous.TERMINE.equals(rdv.getStatut())) {
            throw new OperationNonAutoriseeException("Ce rendez-vous ne peut plus être modifié (statut : " + rdv.getStatut() + ")");
        }
    }

    private Utilisateur trouverUtilisateur(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + email));
    }

    private Utilisateur trouverProfessionnel(Long id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Professionnel introuvable avec l'id : " + id));
    }

    private EtablissementSante trouverEtablissement(Long id) {
        return etablissementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Établissement introuvable avec l'id : " + id));
    }
}
