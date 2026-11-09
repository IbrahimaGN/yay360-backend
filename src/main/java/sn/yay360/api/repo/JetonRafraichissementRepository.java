package sn.yay360.api.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.yay360.api.model.JetonRafraichissement;

import java.util.Optional;

public interface JetonRafraichissementRepository extends JpaRepository<JetonRafraichissement, Long> {

    Optional<JetonRafraichissement> findByJetonHache(String jetonHache);

    @Modifying
    @Query("update JetonRafraichissement j set j.revoque = true where j.famille = :famille")
    int revoquerFamille(@Param("famille") String famille);

    @Modifying
    @Query("update JetonRafraichissement j set j.revoque = true where j.utilisateur.id = :utilisateurId")
    int revoquerTousPourUtilisateur(@Param("utilisateurId") Long utilisateurId);
}
