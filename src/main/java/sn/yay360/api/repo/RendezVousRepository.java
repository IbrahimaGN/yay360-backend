package sn.yay360.api.repo;

import sn.yay360.api.model.RendezVous;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RendezVousRepository extends JpaRepository<RendezVous, Long> {

    List<RendezVous> findByFemmeEnceinteIdOrderByDateHeureDesc(Long femmeEnceinteId);

    List<RendezVous> findByGrossesseIdOrderByDateHeureDesc(Long grossesseId);

    List<RendezVous> findByProfessionnelIdOrderByDateHeureAsc(Long professionnelId);

    List<RendezVous> findByEtablissementIdOrderByDateHeureAsc(Long etablissementId);

    List<RendezVous> findByStatutOrderByDateHeureAsc(String statut);

    List<RendezVous> findByFemmeEnceinteIdAndDateHeureAfterOrderByDateHeureAsc(Long femmeEnceinteId, LocalDateTime apres);

    long countByStatut(String statut);

    long countByGrossesseIdAndDateHeureAfterAndStatutIn(Long grossesseId, LocalDateTime apres, List<String> statuts);

    long countByEtablissementId(Long etablissementId);
}
