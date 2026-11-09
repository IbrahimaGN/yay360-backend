package sn.yay360.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Jeton de rafraichissement (« rester connecté » sur mobile). Rotation à chaque usage : l'ancien
 * jeton est révoqué et un nouveau est émis dans la même famille. La réutilisation d'un jeton
 * révoqué révoque toute la famille (vol de jeton présumé). Seule l'empreinte SHA-256 est stockée.
 */
@Entity
@Table(name = "jetons_rafraichissement")
@Getter
@Setter
@NoArgsConstructor
public class JetonRafraichissement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(name = "jeton_hache", nullable = false, unique = true, length = 64)
    private String jetonHache;

    /** Identifiant commun à tous les jetons issus d'une même connexion. */
    @Column(nullable = false, length = 64)
    private String famille;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(nullable = false)
    private LocalDateTime expiration;

    @Column(nullable = false)
    private Boolean revoque = false;
}
