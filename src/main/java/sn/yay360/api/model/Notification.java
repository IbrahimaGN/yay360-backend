package sn.yay360.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Représente une notification envoyée à un utilisateur (rappels, conseils, alertes) - §5.10.
 * L'envoi réel (SMS/push/email) est simulé en console, comme l'OTP de wallet-service ;
 * cette entité constitue le socle persistant que la couche de notification "réelle"
 * (Module 6 / intégration finale) pourra brancher.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinataire_id", nullable = false)
    private Utilisateur destinataire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grossesse_id")
    private Grossesse grossesse;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(nullable = false, length = 150)
    private String titre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private Boolean lue = false;

    @Column(name = "date_envoi", nullable = false, updatable = false)
    private LocalDateTime dateEnvoi;

    @Column(name = "date_lecture")
    private LocalDateTime dateLecture;

    @PrePersist
    protected void onCreate() {
        this.dateEnvoi = LocalDateTime.now();
        if (this.lue == null) {
            this.lue = false;
        }
    }
}
