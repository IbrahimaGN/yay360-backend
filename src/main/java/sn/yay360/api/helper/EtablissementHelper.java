package sn.yay360.api.helper;

import sn.yay360.api.dto.AffecterProfessionnelRequest;
import sn.yay360.api.dto.EtablissementDto;
import sn.yay360.api.dto.EtablissementRequest;
import sn.yay360.api.exception.OperationNonAutoriseeException;
import sn.yay360.api.exception.ResourceNotFoundException;
import sn.yay360.api.mapper.EtablissementMapper;
import sn.yay360.api.model.EtablissementSante;
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
    private final PerimetreAdminHelper perimetre;

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

    /** Établissements actifs où exerce le soignant connecté (rattachement, planification). */
    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME')")
    public List<EtablissementDto> mesEtablissements(String emailProfessionnel) {
        Utilisateur professionnel = utilisateurRepository.findByEmailIgnoreCase(emailProfessionnel)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        return etablissementRepository.findByProfessionnelsId(professionnel.getId()).stream()
                .filter(e -> Boolean.TRUE.equals(e.getActif()))
                .map(etablissementMapper::versDto)
                .toList();
    }

    /** Établissement géré par l'administrateur d'établissement connecté. */
    @PreAuthorize("hasAuthority('ADMIN_ETABLISSEMENT')")
    public EtablissementDto monEtablissement(String emailAdmin) {
        return etablissementMapper.versDto(perimetre.etablissementAdministre(perimetre.auteur(emailAdmin)));
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

    @PreAuthorize("hasAnyAuthority('ADMIN', 'ADMIN_ETABLISSEMENT')")
    public EtablissementDto mettreAJour(String emailAuteur, Long id, EtablissementRequest request) {
        perimetre.verifierGestionEtablissement(perimetre.auteur(emailAuteur), id);
        EtablissementSante etablissement = trouverParId(id);
        appliquerRequest(etablissement, request);
        return etablissementMapper.versDto(etablissementRepository.save(etablissement));
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'ADMIN_ETABLISSEMENT')")
    public EtablissementDto affecterProfessionnel(String emailAuteur, Long id, AffecterProfessionnelRequest request) {
        perimetre.verifierGestionEtablissement(perimetre.auteur(emailAuteur), id);
        EtablissementSante etablissement = trouverParId(id);
        Utilisateur professionnel = professionnelDesigne(request);

        if (!PerimetreAdminHelper.estSoignant(professionnel)) {
            throw new OperationNonAutoriseeException("Seuls les médecins et sages-femmes peuvent être affectés à un établissement");
        }

        if (!etablissement.getProfessionnels().contains(professionnel)) {
            etablissement.getProfessionnels().add(professionnel);
        }
        return etablissementMapper.versDto(etablissementRepository.save(etablissement));
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'ADMIN_ETABLISSEMENT')")
    public EtablissementDto retirerProfessionnel(String emailAuteur, Long id, Long professionnelId) {
        perimetre.verifierGestionEtablissement(perimetre.auteur(emailAuteur), id);
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
        if (utilisateurRepository.existsByEtablissementAdministreId(id)) {
            throw new OperationNonAutoriseeException(
                    "Cet établissement a un administrateur : désactivez l'établissement ou supprimez d'abord ce compte");
        }
        etablissementRepository.deleteById(id);
    }

    /** Soignant désigné par son id (administrateur de la plateforme) ou par son email. */
    private Utilisateur professionnelDesigne(AffecterProfessionnelRequest request) {
        if (request.getProfessionnelId() != null) {
            return utilisateurRepository.findById(request.getProfessionnelId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Professionnel introuvable avec l'id : " + request.getProfessionnelId()));
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            // Même réponse pour un email inconnu ou non soignant : la recherche ne révèle pas les autres comptes
            return utilisateurRepository.findByEmailIgnoreCase(AuthHelper.normaliserEmail(request.getEmail()))
                    .filter(PerimetreAdminHelper::estSoignant)
                    .orElseThrow(() -> new ResourceNotFoundException("Aucun soignant n'a cet email"));
        }
        throw new IllegalArgumentException("Indiquez l'id ou l'email du soignant");
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
