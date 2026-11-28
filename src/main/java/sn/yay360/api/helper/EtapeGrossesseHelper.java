package sn.yay360.api.helper;

import sn.yay360.api.dto.EtapeGrossesseDto;
import sn.yay360.api.dto.EtapeGrossesseRequest;
import sn.yay360.api.exception.OperationNonAutoriseeException;
import sn.yay360.api.exception.ResourceNotFoundException;
import sn.yay360.api.mapper.EtapeGrossesseMapper;
import sn.yay360.api.model.EtapeGrossesse;
import sn.yay360.api.repo.EtapeGrossesseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gère le contenu de référence par semaine de grossesse (§5.4/§5.5).
 * Contenu entièrement administrable par les professionnels de santé.
 */
@Component
@RequiredArgsConstructor
@Transactional
public class EtapeGrossesseHelper {

    private final EtapeGrossesseRepository etapeGrossesseRepository;
    private final EtapeGrossesseMapper etapeGrossesseMapper;
    private final CalculObstetricalHelper calcul;

    public List<EtapeGrossesseDto> listerToutes() {
        return etapeGrossesseRepository.findAllByOrderBySemaineAsc().stream()
                .filter(etape -> etape.getSemaine() <= CalculObstetricalHelper.DUREE_GROSSESSE_SEMAINES)
                .map(etapeGrossesseMapper::versDto)
                .toList();
    }

    public EtapeGrossesseDto obtenirParSemaine(Integer semaine) {
        if (semaine < 1 || semaine > CalculObstetricalHelper.DUREE_GROSSESSE_SEMAINES) {
            throw new ResourceNotFoundException("Aucun contenu administrable pour la semaine " + semaine);
        }
        EtapeGrossesse etape = trouverParSemaine(semaine);
        return etapeGrossesseMapper.versDto(etape);
    }

    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME', 'ADMIN')")
    public EtapeGrossesseDto creer(EtapeGrossesseRequest request) {
        if (etapeGrossesseRepository.existsBySemaine(request.getSemaine())) {
            throw new OperationNonAutoriseeException("Une étape existe déjà pour la semaine " + request.getSemaine());
        }
        EtapeGrossesse etape = new EtapeGrossesse();
        appliquerRequest(etape, request);
        return etapeGrossesseMapper.versDto(etapeGrossesseRepository.save(etape));
    }

    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME', 'ADMIN')")
    public EtapeGrossesseDto mettreAJour(Long id, EtapeGrossesseRequest request) {
        EtapeGrossesse etape = etapeGrossesseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Étape de grossesse introuvable avec l'id : " + id));
        appliquerRequest(etape, request);
        return etapeGrossesseMapper.versDto(etapeGrossesseRepository.save(etape));
    }

    @PreAuthorize("hasAnyAuthority('MEDECIN', 'SAGE_FEMME', 'ADMIN')")
    public void supprimer(Long id) {
        if (!etapeGrossesseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Étape de grossesse introuvable avec l'id : " + id);
        }
        etapeGrossesseRepository.deleteById(id);
    }

    EtapeGrossesse trouverParSemaine(Integer semaine) {
        return etapeGrossesseRepository.findBySemaine(semaine)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun contenu administrable pour la semaine " + semaine));
    }

    private void appliquerRequest(EtapeGrossesse etape, EtapeGrossesseRequest request) {
        etape.setSemaine(request.getSemaine());
        etape.setTrimestre(calcul.calculerTrimestre(request.getSemaine()));
        etape.setDeveloppementBebe(request.getDeveloppementBebe());
        etape.setChangementsMere(request.getChangementsMere());
        etape.setObjectifsSante(request.getObjectifsSante());
        etape.setConseilsAlimentation(request.getConseilsAlimentation());
        etape.setConseilsHydratation(request.getConseilsHydratation());
        etape.setExercicesPhysiques(request.getExercicesPhysiques());
        etape.setConseilsSommeil(request.getConseilsSommeil());
        etape.setPrecautions(request.getPrecautions());
        etape.setExamensRecommandes(request.getExamensRecommandes());
        etape.setSignesAlerte(request.getSignesAlerte());
    }
}
