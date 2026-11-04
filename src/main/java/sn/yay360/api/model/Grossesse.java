package sn.yay360.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Représente une grossesse suivie sur la plateforme.
 * Les champs calculés (âge gestationnel, semaine, trimestre) ne sont PAS stockés :
 * ils sont recalculés à la volée par GrossesseHelper à partir de dateDernieresRegles,
 * afin de rester toujours exacts (logique "intelligente").
 */
@Entity
@Table(name = "grossesses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Grossesse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "femme_enceinte_id", nullable = false)
    private Utilisateur femmeEnceinte;

    @Column(name = "date_dernieres_regles", nullable = false)
    private LocalDate dateDernieresRegles;

    @Column(name = "date_prevue_accouchement", nullable = false)
    private LocalDate datePrevueAccouchement;

    @Column(nullable = false, length = 20)
    private String statut = StatutGrossesse.EN_COURS;

    @Column(name = "grossesse_a_risque", nullable = false)
    private Boolean grossesseARisque = false;

    @Column(name = "motif_risque", columnDefinition = "TEXT")
    private String motifRisque;

    @Column(name = "nombre_grossesses_precedentes")
    private Integer nombreGrossessesPrecedentes = 0;

    @Column(columnDefinition = "TEXT")
    private String antecedents;

    /** Établissement qui assure le suivi : ses professionnels ont accès au dossier. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etablissement_suivi_id")
    private EtablissementSante etablissementSuivi;

    /** Date de fin de la grossesse (accouchement ou interruption). */
    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
        this.dateModification = LocalDateTime.now();
        if (this.statut == null) {
            this.statut = StatutGrossesse.EN_COURS;
        }
        if (this.grossesseARisque == null) {
            this.grossesseARisque = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.dateModification = LocalDateTime.now();
    }
}
