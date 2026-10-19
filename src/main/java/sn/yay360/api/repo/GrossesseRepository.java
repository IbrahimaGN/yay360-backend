package sn.yay360.api.repo;

import sn.yay360.api.model.Grossesse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GrossesseRepository extends JpaRepository<Grossesse, Long> {

    List<Grossesse> findByFemmeEnceinteIdOrderByDateCreationDesc(Long femmeEnceinteId);

    Optional<Grossesse> findByFemmeEnceinteIdAndStatut(Long femmeEnceinteId, String statut);

    List<Grossesse> findByStatut(String statut);

    List<Grossesse> findAllByOrderByDateCreationDesc();

    List<Grossesse> findByGrossesseARisqueTrue();

    long countByStatut(String statut);

    long countByGrossesseARisqueTrue();
}
