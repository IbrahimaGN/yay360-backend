package sn.yay360.api.repo;

import sn.yay360.api.model.ConseilGrossesse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConseilGrossesseRepository extends JpaRepository<ConseilGrossesse, Long> {

    List<ConseilGrossesse> findByActifTrueAndSemaineDebutLessThanEqualAndSemaineFinGreaterThanEqual(
            Integer semaine1, Integer semaine2);

    List<ConseilGrossesse> findByActifTrueAndCategorie(String categorie);

    List<ConseilGrossesse> findByCategorie(String categorie);
}
