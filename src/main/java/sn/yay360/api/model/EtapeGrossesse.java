package sn.yay360.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Contenu de référence pour chaque semaine de grossesse (1 à 40).
 * Entièrement administrable par les professionnels de santé (§5.4 / §5.5 du cahier des charges).
 */
@Entity
@Table(name = "etapes_grossesse")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EtapeGrossesse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer semaine;

    @Column(nullable = false, length = 5)
    private String trimestre;

    @Column(name = "developpement_bebe", columnDefinition = "TEXT")
    private String developpementBebe;

    @Column(name = "changements_mere", columnDefinition = "TEXT")
    private String changementsMere;

    @Column(name = "objectifs_sante", columnDefinition = "TEXT")
    private String objectifsSante;

    @Column(name = "conseils_alimentation", columnDefinition = "TEXT")
    private String conseilsAlimentation;

    @Column(name = "conseils_hydratation", columnDefinition = "TEXT")
    private String conseilsHydratation;

    @Column(name = "exercices_physiques", columnDefinition = "TEXT")
    private String exercicesPhysiques;

    @Column(name = "conseils_sommeil", columnDefinition = "TEXT")
    private String conseilsSommeil;

    @Column(columnDefinition = "TEXT")
    private String precautions;

    @Column(name = "examens_recommandes", columnDefinition = "TEXT")
    private String examensRecommandes;

    @Column(name = "signes_alerte", columnDefinition = "TEXT")
    private String signesAlerte;
}
