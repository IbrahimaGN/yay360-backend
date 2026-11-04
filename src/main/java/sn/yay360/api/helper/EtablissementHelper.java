package sn.yay360.api.helper;

import sn.yay360.api.dto.AffecterProfessionnelRequest;
import sn.yay360.api.dto.EtablissementDto;
import sn.yay360.api.dto.EtablissementRequest;
import sn.yay360.api.exception.OperationNonAutoriseeException;
import sn.yay360.api.exception.ResourceNotFoundException;
import sn.yay360.api.mapper.EtablissementMapper;
import sn.yay360.api.model.EtablissementSante;
import sn.yay360.api.model.RoleUtilisateur;
import sn.yay360.api.model.Utilisateur;
import sn.yay360.api.repo.EtablissementSanteRepository;
import sn.yay360.api.repo.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Contient toute la logique métier liée à la gestion des établissements de santé (§5.8).
 */
@Component
@RequiredArgsConstructor
@Transactional
public class EtablissementHelper {

    private final EtablissementSanteRepository etablissementRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EtablissementMapper etablissementMapper;

    public List<EtablissementDto> listerActifs() {
        return etablissementRepository.findByActifTrue().stream()
                .map(etablissementMapper::versDto)
                .toList();
    }

    public List<EtablissementDto> listerParVille(String ville) {
        return etablissementRepository.findByVilleIgnoreCaseAndActifTrue(ville).stream()
                .map(etablissementMapper::versDto)
                .toList();
    }

    public EtablissementDto obtenirParId(Long id) {
        return etablissementMapper.versDto(trouverParId(id));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    public List<EtablissementDto> listerTous() {
        return etablissementRepository.findAll().stream()
                .map(etablissementMapper::versDto)
                .toList();
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    public EtablissementDto creer(EtablissementRequest request) {
        EtablissementSante etablissement = new EtablissementSante();
        appliquerRequest(etablissement, request);
        etablissement.setActif(true);
        return etablissementMapper.versDto(etablissementRepository.save(etablissement));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    public EtablissementDto mettreAJour(Long id, EtablissementRequest request) {
        EtablissementSante etablissement = trouverParId(id);
        appliquerRequest(etablissement, request);
        return etablissementMapper.versDto(etablissementRepository.save(etablissement));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    public EtablissementDto affecterProfessionnel(Long id, AffecterProfessionnelRequest request) {
        EtablissementSante etablissement = trouverParId(id);
        Utilisateur professionnel = utilisateurRepository.findById(request.getProfessionnelId())
                .orElseThrow(() -> new ResourceNotFoundException("Professionnel introuvable avec l'id : " + request.getProfessionnelId()));

        boolean estProfessionnelSante = RoleUtilisateur.MEDECIN.equals(professionnel.getRole())
                || RoleUtilisateur.SAGE_FEMME.equals(professionnel.getRole());
        if (!estProfessionnelSante) {
            throw new OperationNonAutoriseeException("Seuls les médecins et sages-femmes peuvent être affectés à un établissement");
        }

        if (!etablissement.getProfessionnels().contains(professionnel)) {
            etablissement.getProfessionnels().add(professionnel);
        }
        return etablissementMapper.versDto(etablissementRepository.save(etablissement));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    public EtablissementDto retirerProfessionnel(Long id, Long professionnelId) {
        EtablissementSante etablissement = trouverParId(id);
        etablissement.getProfessionnels().removeIf(p -> p.getId().equals(professionnelId));
        return etablissementMapper.versDto(etablissementRepository.save(etablissement));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    public void desactiver(Long id) {
        EtablissementSante etablissement = trouverParId(id);
        etablissement.setActif(false);
        etablissementRepository.save(etablissement);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    public void supprimer(Long id) {
        if (!etablissementRepository.existsById(id)) {
            throw new ResourceNotFoundException("Établissement introuvable avec l'id : " + id);
        }
        etablissementRepository.deleteById(id);
    }

    private void appliquerRequest(EtablissementSante etablissement, EtablissementRequest request) {
        etablissement.setNom(request.getNom());
        etablissement.setType(request.getType());
        etablissement.setAdresse(request.getAdresse());
        etablissement.setVille(request.getVille());
        etablissement.setTelephone(request.getTelephone());
        etablissement.setEmail(request.getEmail());
        etablissement.setLatitude(request.getLatitude());
        etablissement.setLongitude(request.getLongitude());
    }

    private EtablissementSante trouverParId(Long id) {
        return etablissementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Établissement introuvable avec l'id : " + id));
    }
}
