package sn.yay360.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Code à usage unique envoyé pour vérifier un numéro de téléphone (connexion mobile).
 * Le code est haché (BCrypt) ; après vérification, il donne droit à un jeton court
 * permettant de définir le PIN, lui aussi haché (SHA-256).
 */
@Entity
@Table(name = "codes_verification")
@Getter
@Setter
@NoArgsConstructor
public class CodeVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String telephone;

    /** Compte concerné ; null tant qu'une inscription n'est pas confirmée. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @Column(name = "code_hache", nullable = false, length = 100)
    private String codeHache;

    @Column(name = "date_envoi", nullable = false)
    private LocalDateTime dateEnvoi;

    @Column(nullable = false)
    private LocalDateTime expiration;

    @Column(nullable = false)
    private Integer essais = 0;

    @Column(nullable = false)
    private Boolean utilise = false;

    @Column(name = "insc_nom", length = 100)
    private String inscNom;

    @Column(name = "insc_prenom", length = 100)
    private String inscPrenom;

    @Column(name = "insc_email", length = 150)
    private String inscEmail;

    @Column(name = "insc_date_naissance")
    private LocalDate inscDateNaissance;

    @Column(name = "jeton_pin_hache", length = 64)
    private String jetonPinHache;

    @Column(name = "jeton_pin_expiration")
    private LocalDateTime jetonPinExpiration;
}
