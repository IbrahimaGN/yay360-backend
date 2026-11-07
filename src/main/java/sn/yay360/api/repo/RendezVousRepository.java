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

    @org.springframework.data.jpa.repository.Query(
            "select r.dateHeure from RendezVous r where r.grossesse.id = :grossesseId and r.statut in ('PLANIFIE', 'CONFIRME')")
    List<LocalDateTime> datesActivesParGrossesse(
            @org.springframework.data.repository.query.Param("grossesseId") Long grossesseId);

    @org.springframework.data.jpa.repository.Query("select r.statut, count(r) from RendezVous r group by r.statut")
    List<Object[]> compterParStatut();

    long countByGrossesseIdAndDateHeureAfterAndStatutIn(Long grossesseId, LocalDateTime apres, List<String> statuts);

    long countByEtablissementId(Long etablissementId);

    List<RendezVous> findByStatutAndGrossesseIdInOrderByDateHeureAsc(String statut, List<Long> grossesseIds);

    boolean existsByProfessionnelIdAndDateHeureAndStatutIn(Long professionnelId, LocalDateTime dateHeure,
                                                           List<String> statuts);

    /** Rendez-vous à rappeler : fenêtre [debut, fin[, statuts actifs, relations chargées (pas de N+1). */
    @org.springframework.data.jpa.repository.Query("""
            select r from RendezVous r
            join fetch r.femmeEnceinte
            join fetch r.grossesse
            where r.dateHeure >= :debut and r.dateHeure < :fin and r.statut in :statuts
              and (r.rappelEnvoye is null or r.rappelEnvoye = false)
              and r.grossesse.statut = 'EN_COURS'
            """)
    List<RendezVous> findARappeler(@org.springframework.data.repository.query.Param("debut") LocalDateTime debut,
                                   @org.springframework.data.repository.query.Param("fin") LocalDateTime fin,
                                   @org.springframework.data.repository.query.Param("statuts") List<String> statuts);
}
