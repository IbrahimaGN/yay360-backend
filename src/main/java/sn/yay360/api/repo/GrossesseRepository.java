package sn.yay360.api.repo;

import sn.yay360.api.model.Grossesse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GrossesseRepository extends JpaRepository<Grossesse, Long> {

    List<Grossesse> findByFemmeEnceinteIdOrderByDateCreationDesc(Long femmeEnceinteId);

    Optional<Grossesse> findByFemmeEnceinteIdAndStatut(Long femmeEnceinteId, String statut);

    List<Grossesse> findByStatut(String statut);

    List<Grossesse> findAllByOrderByDateCreationDesc();

    List<Grossesse> findByGrossesseARisqueTrue();

    long countByStatut(String statut);

    @Query("select g.statut, count(g) from Grossesse g group by g.statut")
    List<Object[]> compterParStatut();

    @Query("select g.dateDernieresRegles from Grossesse g where g.statut = 'EN_COURS'")
    List<java.time.LocalDate> datesDernieresReglesEnCours();

    long countByGrossesseARisqueTrue();

    long countByStatutAndGrossesseARisqueTrue(String statut);

    // --- Tableau de bord d'un établissement (grossesses dont il est l'établissement de suivi) ---

    @Query("select g.statut, count(g) from Grossesse g where g.etablissementSuivi.id = :etablissementId group by g.statut")
    List<Object[]> compterParStatutDansEtablissement(@Param("etablissementId") Long etablissementId);

    long countByStatutAndGrossesseARisqueTrueAndEtablissementSuiviId(String statut, Long etablissementId);

    @Query("select g.dateDernieresRegles from Grossesse g where g.statut = 'EN_COURS' and g.etablissementSuivi.id = :etablissementId")
    List<java.time.LocalDate> datesDernieresReglesEnCoursDansEtablissement(@Param("etablissementId") Long etablissementId);

    @Query("select count(distinct g.femmeEnceinte.id) from Grossesse g where g.etablissementSuivi.id = :etablissementId")
    long compterPatientesDansEtablissement(@Param("etablissementId") Long etablissementId);

    /**
     * Grossesses d'un statut donné suivies par un professionnel : établissement de suivi
     * dont il est membre, ou lien de soin direct (RDV assigné, consultation, alerte prise en charge).
     */
    @Query("""
            select distinct g from Grossesse g
            left join g.etablissementSuivi e
            where g.statut = :statut and (
                exists (select 1 from EtablissementSante es join es.professionnels p
                        where es = e and p.id = :professionnelId)
                or exists (select 1 from RendezVous r where r.grossesse = g and r.professionnel.id = :professionnelId
                           and r.statut <> 'ANNULE')
                or exists (select 1 from Consultation c where c.grossesse = g and c.professionnel.id = :professionnelId)
                or exists (select 1 from AlerteMedicale a where a.grossesse = g
                           and a.professionnelPriseEnCharge.id = :professionnelId)
            )
            order by g.dateCreation desc
            """)
    List<Grossesse> findSuiviesParProfessionnel(@Param("professionnelId") Long professionnelId,
                                                @Param("statut") String statut);

    @Query("""
            select case when count(g) > 0 then true else false end from Grossesse g
            left join g.etablissementSuivi e
            where g.id = :grossesseId and (
                exists (select 1 from EtablissementSante es join es.professionnels p
                        where es = e and p.id = :professionnelId)
                or exists (select 1 from RendezVous r where r.grossesse = g and r.professionnel.id = :professionnelId
                           and r.statut <> 'ANNULE')
                or exists (select 1 from Consultation c where c.grossesse = g and c.professionnel.id = :professionnelId)
                or exists (select 1 from AlerteMedicale a where a.grossesse = g
                           and a.professionnelPriseEnCharge.id = :professionnelId)
            )
            """)
    boolean estSuivieParProfessionnel(@Param("grossesseId") Long grossesseId,
                                      @Param("professionnelId") Long professionnelId);

}
