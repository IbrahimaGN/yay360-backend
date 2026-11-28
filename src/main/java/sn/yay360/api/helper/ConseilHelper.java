package sn.yay360.api.helper;

import sn.yay360.api.dto.ConseilDto;
import sn.yay360.api.dto.ConseilRequest;
import sn.yay360.api.exception.ResourceNotFoundException;
import sn.yay360.api.mapper.ConseilMapper;
import sn.yay360.api.model.ConseilGrossesse;
import sn.yay360.api.model.Utilisateur;
import sn.yay360.api.repo.ConseilGrossesseRepository;
import sn.yay360.api.repo.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gère la bibliothèque de conseils classés par semaine/trimestre/catégorie (§5.9).
 */
@Component
@RequiredArgsConstructor
@Transactional
public class ConseilHelper {

    private final ConseilGrossesseRepository conseilRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ConseilMapper conseilMapper;
    private final CalculObstetricalHelper calcul;

    public List<ConseilDto> obtenirPourSemaine(Integer semaine) {
        if (semaine < 1 || semaine > CalculObstetricalHelper.DUREE_GROSSESSE_SEMAINES) {
            return List.of();
        }
        return conseilRepository
                .findByActifTrueAndSemaineDebutLessThanEqualAndSemaineFinGreaterThanEqual(semaine, semaine).stream()
                .map(this::versDtoDansLimite)
                .toList();
    }

    public List<ConseilDto> obtenirParCategorie(String categorie) {
        return conseilRepository.findByActifTrueAndCategorie(categorie).stream()
                .filter(conseil -> conseil.getSemaineDebut() <= CalculObstetricalHelper.DUREE_GROSSESSE_SEMAINES)
                .map(this::versDtoDansLimite)
                .toList();
    }

    public List<ConseilDto> listerTous() {
        return conseilRepository.findAll().stream()
                .filter(conseil -> conseil.getSemaineDebut() <= CalculObstetricalHelper.DUREE_GROSSESSE_SEMAINES)
                .map(this::versDtoDansLimite)
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME', 'ADMIN')")
    public ConseilDto creer(String emailAuteur, ConseilRequest request) {
        Utilisateur auteur = utilisateurRepository.findByEmailIgnoreCase(emailAuteur)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + emailAuteur));

        if (request.getSemaineDebut() > request.getSemaineFin()) {
            throw new IllegalArgumentException("La semaine de début doit être inférieure ou égale à la semaine de fin");
        }

        ConseilGrossesse conseil = new ConseilGrossesse();
        conseil.setSemaineDebut(request.getSemaineDebut());
        conseil.setSemaineFin(request.getSemaineFin());
        conseil.setTrimestre(calcul.calculerTrimestre(request.getSemaineDebut()));
        conseil.setCategorie(request.getCategorie());
        conseil.setTitre(request.getTitre());
        conseil.setContenu(request.getContenu());
        conseil.setActif(true);
        conseil.setAuteur(auteur);

        return conseilMapper.versDto(conseilRepository.save(conseil));
    }

    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME', 'ADMIN')")
    public ConseilDto mettreAJour(Long id, ConseilRequest request) {
        ConseilGrossesse conseil = trouverParId(id);
        conseil.setSemaineDebut(request.getSemaineDebut());
        conseil.setSemaineFin(request.getSemaineFin());
        conseil.setTrimestre(calcul.calculerTrimestre(request.getSemaineDebut()));
        conseil.setCategorie(request.getCategorie());
        conseil.setTitre(request.getTitre());
        conseil.setContenu(request.getContenu());
        return conseilMapper.versDto(conseilRepository.save(conseil));
    }

    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME', 'ADMIN')")
    public ConseilDto basculerActivation(Long id) {
        ConseilGrossesse conseil = trouverParId(id);
        conseil.setActif(!Boolean.TRUE.equals(conseil.getActif()));
        return conseilMapper.versDto(conseilRepository.save(conseil));
    }

    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME', 'ADMIN')")
    public void supprimer(Long id) {
        if (!conseilRepository.existsById(id)) {
            throw new ResourceNotFoundException("Conseil introuvable avec l'id : " + id);
        }
        conseilRepository.deleteById(id);
    }

    private ConseilGrossesse trouverParId(Long id) {
        return conseilRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conseil introuvable avec l'id : " + id));
    }

    private ConseilDto versDtoDansLimite(ConseilGrossesse conseil) {
        ConseilDto dto = conseilMapper.versDto(conseil);
        if (dto != null && dto.getSemaineFin() != null) {
            dto.setSemaineFin(Math.min(dto.getSemaineFin(), CalculObstetricalHelper.DUREE_GROSSESSE_SEMAINES));
        }
        return dto;
    }
}
