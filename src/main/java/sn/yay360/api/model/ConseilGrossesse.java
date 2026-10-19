package sn.yay360.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Bibliothèque de conseils classés par semaines/trimestre/catégorie (§5.9).
 * Chaque conseil peut être activé/désactivé/mis à jour par un professionnel autorisé.
 */
@Entity
@Table(name = "conseils_grossesse")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConseilGrossesse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "semaine_debut", nullable = false)
    private Integer semaineDebut;

    @Column(name = "semaine_fin", nullable = false)
    private Integer semaineFin;

    @Column(length = 5)
    private String trimestre;

    @Column(nullable = false, length = 40)
    private String categorie;

    @Column(nullable = false, length = 150)
    private String titre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenu;

    @Column(nullable = false)
    private Boolean actif = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auteur_id")
    private Utilisateur auteur;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
        this.dateModification = LocalDateTime.now();
        if (this.actif == null) {
            this.actif = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.dateModification = LocalDateTime.now();
    }
}
