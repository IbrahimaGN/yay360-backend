package sn.yay360.api.repo;

import sn.yay360.api.model.EtablissementSante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EtablissementSanteRepository extends JpaRepository<EtablissementSante, Long> {

    List<EtablissementSante> findByActifTrue();

    List<EtablissementSante> findByVilleIgnoreCaseAndActifTrue(String ville);

    List<EtablissementSante> findByProfessionnelsId(Long professionnelId);
}
