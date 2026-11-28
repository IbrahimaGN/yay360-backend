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

    /**
     * Alertes NOUVELLES de patientes qu'aucun soignant actif ne suit (même règle que
     * UtilisateurRepository.findSoignantsSuivant) : elles sont visibles de tous les soignants.
     */
    @org.springframework.data.jpa.repository.Query("""
            select a from AlerteMedicale a join a.grossesse g left join g.etablissementSuivi e
            where a.statut = 'NOUVELLE' and not exists (
                select 1 from Utilisateur u
                where u.actif = true and u.role in ('MEDECIN', 'SAGE_FEMME') and (
                    exists (select 1 from EtablissementSante es join es.professionnels p where es = e and p = u)
                    or exists (select 1 from RendezVous r where r.grossesse = g and r.professionnel = u
                               and r.statut <> 'ANNULE')
                    or exists (select 1 from Consultation c where c.grossesse = g and c.professionnel = u)
                    or exists (select 1 from AlerteMedicale a2 where a2.grossesse = g
                               and a2.professionnelPriseEnCharge = u)
                )
            )
            order by a.dateDeclaration desc
            """)
    List<AlerteMedicale> findNouvellesSansSuivi();

    long countByGrossesseIdAndNiveauGravite(Long grossesseId, String niveauGravite);

    @org.springframework.data.jpa.repository.Query("select a.statut, count(a) from AlerteMedicale a group by a.statut")
    List<Object[]> compterParStatut();

    @org.springframework.data.jpa.repository.Query(
            "select a.niveauGravite, count(a) from AlerteMedicale a where a.statut <> 'RESOLUE' group by a.niveauGravite")
    List<Object[]> compterNonResoluesParGravite();

    /** Couples (déclaration, prise en charge) pour le délai moyen de réaction des soignants. */
    @org.springframework.data.jpa.repository.Query(
            "select a.dateDeclaration, a.datePriseEnCharge from AlerteMedicale a where a.datePriseEnCharge is not null")
    List<Object[]> delaisPriseEnCharge();

    List<AlerteMedicale> findByStatutAndGrossesseIdInOrderByDateDeclarationDesc(String statut, List<Long> grossesseIds);

    List<AlerteMedicale> findByNiveauGraviteAndStatutNotAndGrossesseIdInOrderByDateDeclarationDesc(
            String niveauGravite, String statut, List<Long> grossesseIds);

    List<AlerteMedicale> findByNiveauGraviteAndStatutNotOrderByDateDeclarationDesc(String niveauGravite, String statut);

    long countByNiveauGraviteAndStatutNot(String niveauGravite, String statut);

    /** Alertes récentes d'une grossesse (détection de récurrence pour le triage contextuel). */
    long countByGrossesseIdAndDateDeclarationAfter(Long grossesseId, java.time.LocalDateTime depuis);
}
