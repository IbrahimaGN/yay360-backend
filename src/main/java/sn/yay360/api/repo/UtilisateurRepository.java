package sn.yay360.api.repo;

import sn.yay360.api.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByEmail(String email);

    Optional<Utilisateur> findByEmailIgnoreCase(String email);

    /**
     * Lecture verrouillée (SELECT … FOR UPDATE) pour la connexion : les tentatives simultanées
     * sur un même compte sont sérialisées, le compteur d'échecs ne peut pas être contourné.
     */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Utilisateur u where lower(u.email) = lower(:email)")
    Optional<Utilisateur> findPourConnexion(@Param("email") String email);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<Utilisateur> findByResetToken(String resetToken);

    Optional<Utilisateur> findByFemmeAssocieeId(Long femmeAssocieeId);

    List<Utilisateur> findByRole(String role);

    List<Utilisateur> findByTelephoneAndRole(String telephone, String role);

    /** Comptes mobiles (patiente, conjoint) associés à un numéro, pour la connexion par PIN. */
    List<Utilisateur> findByTelephoneAndRoleIn(String telephone, java.util.Collection<String> roles);

    List<Utilisateur> findByRoleInAndActifTrue(List<String> roles);

    /** Soignants actifs qui suivent une grossesse (établissement de suivi ou lien de soin direct). */
    @Query("""
            select distinct u from Utilisateur u
            where u.actif = true and u.role in ('MEDECIN', 'SAGE_FEMME') and (
                exists (select 1 from Grossesse g join g.etablissementSuivi e join e.professionnels p
                        where g.id = :grossesseId and p = u)
                or exists (select 1 from RendezVous r where r.grossesse.id = :grossesseId and r.professionnel = u
                           and r.statut <> 'ANNULE')
                or exists (select 1 from Consultation c where c.grossesse.id = :grossesseId and c.professionnel = u)
                or exists (select 1 from AlerteMedicale a where a.grossesse.id = :grossesseId
                           and a.professionnelPriseEnCharge = u)
            )
            """)
    List<Utilisateur> findSoignantsSuivant(@Param("grossesseId") Long grossesseId);

    List<Utilisateur> findByActif(Boolean actif);

    long countByRole(String role);

    @Query("select u.role, count(u) from Utilisateur u group by u.role")
    List<Object[]> compterParRole();
}
