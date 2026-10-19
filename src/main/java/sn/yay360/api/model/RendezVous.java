package sn.yay360.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Représente un rendez-vous médical (§5.7).
 */
@Entity
@Table(name = "rendez_vous")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RendezVous {

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
    @JoinColumn(name = "professionnel_id")
    private Utilisateur professionnel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etablissement_id")
    private EtablissementSante etablissement;

    @Column(name = "date_heure", nullable = false)
    private LocalDateTime dateHeure;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(nullable = false, length = 20)
    private String statut = StatutRendezVous.PLANIFIE;

    @Column(columnDefinition = "TEXT")
    private String motif;

    @Column(name = "notes_annulation", columnDefinition = "TEXT")
    private String notesAnnulation;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
        this.dateModification = LocalDateTime.now();
        if (this.statut == null) {
            this.statut = StatutRendezVous.PLANIFIE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.dateModification = LocalDateTime.now();
    }
}
