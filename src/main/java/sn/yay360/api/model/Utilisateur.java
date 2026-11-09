package sn.yay360.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entité représentant un utilisateur de la plateforme YAY360.
 * Regroupe Admin, Femme enceinte, conjoint, Sage-femme et Médecin.
 * Le rôle discrimine le comportement métier (géré au niveau des Helpers).
 */
@Entity
@Table(name = "utilisateurs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "mot_de_passe", nullable = false)
    private String motDePasse;

    @Column(length = 20)
    private String telephone;

    @Column(name = "date_naissance", nullable = false)
    private LocalDate dateNaissance;

    @Column(nullable = false, length = 30)
    private String role;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "femme_associee_id", unique = true)
    private Utilisateur femmeAssociee;

    @Column(nullable = false)
    private Boolean actif = true;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    // --- Réinitialisation de mot de passe (seule l'empreinte SHA-256 du jeton est stockée) ---
    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "reset_token_expiration")
    private LocalDateTime resetTokenExpiration;

    // --- Sécurité de l'authentification ---
    /** Les JWT émis avant cette date sont refusés (changement / réinitialisation du mot de passe). */
    @Column(name = "date_changement_mot_de_passe")
    private LocalDateTime dateChangementMotDePasse;

    /**
     * Version des jetons : incrémentée à chaque changement de mot de passe. Un JWT portant
     * une version différente est refusé (révocation immédiate de toutes les sessions).
     */
    @Column(name = "version_jeton")
    private Integer versionJeton = 0;

    /** Mot de passe initial généré par l'admin : à changer à la première connexion. */
    @Column(name = "mot_de_passe_temporaire")
    private Boolean motDePasseTemporaire = false;

    @Column(name = "tentatives_connexion_echouees")
    private Integer tentativesConnexionEchouees = 0;

    @Column(name = "verrouille_jusqua")
    private LocalDateTime verrouilleJusqua;

    // ---- Connexion mobile par PIN (téléphone + PIN à 4 chiffres) ----

    /** PIN haché (BCrypt) ; null tant qu'il n'a pas été défini après vérification du téléphone. */
    @Column(name = "pin", length = 100)
    private String pin;

    @Column(name = "pin_echecs", nullable = false)
    private Integer pinEchecs = 0;

    @Column(name = "pin_verrouille_jusqua")
    private LocalDateTime pinVerrouilleJusqua;

    /** Verrouillages successifs : au-delà du seuil, le PIN est effacé et un nouveau code est exigé. */
    @Column(name = "pin_verrouillages", nullable = false)
    private Integer pinVerrouillages = 0;

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
