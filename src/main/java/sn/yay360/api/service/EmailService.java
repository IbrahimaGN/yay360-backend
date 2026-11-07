package sn.yay360.api.service;

/**
 * Envoi des emails transactionnels (réinitialisation de mot de passe, invitation du conjoint,
 * création de compte professionnel).
 */
public interface EmailService {

    /**
     * Envoie un email texte. Ne lève jamais d'exception : un échec d'envoi est journalisé
     * sans bloquer l'opération métier.
     *
     * @param lienSensible lien contenant un jeton à usage unique ; il n'est journalisé que si
     *                     l'option de développement {@code yay360.mail.journaliser-liens} est active
     *                     et qu'aucun serveur SMTP n'est configuré.
     */
    void envoyer(String destinataire, String sujet, String corps, String lienSensible);

    /** Indique si un serveur SMTP réel est configuré. */
    boolean estConfigure();
}
