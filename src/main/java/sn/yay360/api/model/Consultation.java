package sn.yay360.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Représente une consultation médicale enregistrée (§5.6) :
 * constantes médicales, observations, diagnostic, traitement, prescriptions.
 */
@Entity
@Table(name = "consultations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grossesse_id", nullable = false)
    private Grossesse grossesse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "femme_enceinte_id", nullable = false)
    private Utilisateur femmeEnceinte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professionnel_id", nullable = false)
    private Utilisateur professionnel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rendez_vous_id")
    private RendezVous rendezVous;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etablissement_id")
    private EtablissementSante etablissement;

    @Column(name = "date_consultation", nullable = false)
    private LocalDateTime dateConsultation;

    // --- Constantes médicales ---
    @Column(name = "poids_kg")
    private Double poidsKg;

    @Column(name = "tension_arterielle", length = 20)
    private String tensionArterielle;

    @Column(name = "temperature_c")
    private Double temperatureC;

    @Column(name = "hauteur_uterine_cm")
    private Double hauteurUterineCm;

    @Column(name = "battements_coeur_foetal")
    private Integer battementsCoeurFoetal;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(columnDefinition = "TEXT")
    private String diagnostic;

    @Column(columnDefinition = "TEXT")
    private String traitement;

    @Column(columnDefinition = "TEXT")
    private String prescriptions;

    /** Signaux détectés automatiquement sur les constantes (un par ligne, « CODE|NIVEAU|message »). */
    @Column(name = "signaux_cliniques", columnDefinition = "TEXT")
    private String signauxCliniques;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
        if (this.dateConsultation == null) {
            this.dateConsultation = LocalDateTime.now();
        }
    }
}
