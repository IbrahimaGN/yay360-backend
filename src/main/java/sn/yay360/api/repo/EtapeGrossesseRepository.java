package sn.yay360.api.repo;

import sn.yay360.api.model.EtapeGrossesse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EtapeGrossesseRepository extends JpaRepository<EtapeGrossesse, Long> {

    Optional<EtapeGrossesse> findBySemaine(Integer semaine);

    boolean existsBySemaine(Integer semaine);

    List<EtapeGrossesse> findAllByOrderBySemaineAsc();
}
