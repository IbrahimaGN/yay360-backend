package sn.yay360.api.repo;

import sn.yay360.api.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<Utilisateur> findByResetToken(String resetToken);

    Optional<Utilisateur> findByFemmeAssocieeId(Long femmeAssocieeId);

    List<Utilisateur> findByRole(String role);

    List<Utilisateur> findByActif(Boolean actif);

    long countByRole(String role);
}
