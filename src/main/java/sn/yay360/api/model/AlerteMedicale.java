package sn.yay360.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Représente une alerte médicale générée lorsqu'une femme enceinte déclare
 * des symptômes nécessitant une vigilance particulière (§5.11).
 */
@Entity
@Table(name = "alertes_medicales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlerteMedicale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grossesse_id", nullable = false)
    private Grossesse grossesse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "femme_enceinte_id", nullable = false)
    private Utilisateur femmeEnceinte;

    @Column(name = "type_symptome", nullable = false, length = 50)
    private String typeSymptome;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "niveau_gravite", nullable = false, length = 20)
    private String niveauGravite;

    @Column(nullable = false, length = 20)
    private String statut = StatutAlerte.NOUVELLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professionnel_id")
    private Utilisateur professionnelPriseEnCharge;

    @Column(name = "notes_prise_en_charge", columnDefinition = "TEXT")
    private String notesPriseEnCharge;

    @Column(name = "date_declaration", nullable = false, updatable = false)
    private LocalDateTime dateDeclaration;

    @Column(name = "date_resolution")
    private LocalDateTime dateResolution;

    @PrePersist
    protected void onCreate() {
        this.dateDeclaration = LocalDateTime.now();
        if (this.statut == null) {
            this.statut = StatutAlerte.NOUVELLE;
        }
    }
}
