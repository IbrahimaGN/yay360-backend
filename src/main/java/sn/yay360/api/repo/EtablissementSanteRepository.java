package sn.yay360.api.repo;

import sn.yay360.api.model.EtablissementSante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EtablissementSanteRepository extends JpaRepository<EtablissementSante, Long> {

    List<EtablissementSante> findByActifTrue();

    List<EtablissementSante> findByVilleIgnoreCaseAndActifTrue(String ville);

    List<EtablissementSante> findByProfessionnelsId(Long professionnelId);

    boolean existsByIdAndProfessionnelsId(Long etablissementId, Long professionnelId);

    /** Activité des établissements actifs (nombre de rendez-vous) en une seule requête. */
    @org.springframework.data.jpa.repository.Query("""
            select e.id, e.nom, e.ville, count(r) from EtablissementSante e
            left join RendezVous r on r.etablissement = e
            where e.actif = true
            group by e.id, e.nom, e.ville
            order by count(r) desc
            """)
    List<Object[]> activiteEtablissementsActifs();
}
