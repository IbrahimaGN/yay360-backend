package sn.yay360.api.repo;

import sn.yay360.api.model.AlerteMedicale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlerteMedicaleRepository extends JpaRepository<AlerteMedicale, Long> {

    List<AlerteMedicale> findByFemmeEnceinteIdOrderByDateDeclarationDesc(Long femmeEnceinteId);

    List<AlerteMedicale> findByGrossesseIdOrderByDateDeclarationDesc(Long grossesseId);

    List<AlerteMedicale> findByStatutOrderByDateDeclarationDesc(String statut);

    List<AlerteMedicale> findByNiveauGraviteOrderByDateDeclarationDesc(String niveauGravite);

    long countByGrossesseIdAndStatutNot(Long grossesseId, String statut);

    List<AlerteMedicale> findByProfessionnelPriseEnChargeIdOrderByDateDeclarationDesc(Long professionnelId);

    long countByStatut(String statut);
}
