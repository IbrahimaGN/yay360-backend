package sn.yay360.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Représente un établissement de santé (centre de santé, hôpital, clinique) - §5.8.
 */
@Entity
@Table(name = "etablissements_sante")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EtablissementSante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(nullable = false, length = 50)
    private String type; // HOPITAL, CLINIQUE, CENTRE_DE_SANTE, POSTE_DE_SANTE

    @Column(nullable = false, length = 200)
    private String adresse;

    @Column(length = 100)
    private String ville;

    @Column(length = 20)
    private String telephone;

    @Column(length = 150)
    private String email;

    private Double latitude;

    private Double longitude;

    @Column(nullable = false)
    private Boolean actif = true;

    @ManyToMany
    @JoinTable(
            name = "etablissement_professionnels",
            joinColumns = @JoinColumn(name = "etablissement_id"),
            inverseJoinColumns = @JoinColumn(name = "professionnel_id")
    )
    private List<Utilisateur> professionnels = new ArrayList<>();

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
