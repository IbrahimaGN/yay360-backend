package sn.yay360.api.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import sn.yay360.api.service.EmailService;

import java.util.List;
import java.util.Map;

/**
 * Envoi des emails, par ordre de priorité :
 * <ol>
 *   <li><b>API HTTPS de Brevo</b> si {@code yay360.mail.brevo-api-key} est renseignée : seul canal possible sur
 *       l'offre gratuite de Render, qui bloque les ports SMTP ;</li>
 *   <li><b>SMTP</b> si {@code spring.mail.host} est renseigné (poste de développement, autre hébergeur) ;</li>
 *   <li>sinon aucun envoi ; en développement uniquement, le lien ou le code peut être journalisé
 *       ({@code YAY360_MAIL_JOURNALISER_LIENS=true}).</li>
 * </ol>
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    static final String URL_BREVO = "https://api.brevo.com/v3/smtp/email";

    private final JavaMailSender mailSender;
    private final RestClient brevo;
    private final String cleBrevo;
    private final String expediteur;
    private final String expediteurNom;
    private final boolean journaliserLiens;

    public EmailServiceImpl(ObjectProvider<JavaMailSender> mailSender,
                            RestClient.Builder restClient,
                            @Value("${spring.mail.host:}") String hoteSmtp,
                            @Value("${yay360.mail.brevo-api-key:}") String cleBrevo,
                            @Value("${yay360.mail.from:no-reply@yay360.sn}") String expediteur,
                            @Value("${yay360.mail.expediteur-nom:YAY360}") String expediteurNom,
                            @Value("${yay360.mail.journaliser-liens:false}") boolean journaliserLiens) {
        // Un hôte vide (variable non renseignée) équivaut à « pas de SMTP ».
        this.mailSender = hoteSmtp == null || hoteSmtp.isBlank() ? null : mailSender.getIfAvailable();
        this.cleBrevo = cleBrevo == null ? "" : cleBrevo.trim();
        this.brevo = restClient.build();
        this.expediteur = expediteur;
        this.expediteurNom = expediteurNom;
        this.journaliserLiens = journaliserLiens;
    }

    @Override
    public boolean estConfigure() {
        return !cleBrevo.isEmpty() || mailSender != null;
    }

    /** Asynchrone : le temps de réponse ne révèle pas si un email a été envoyé (pas d'énumération). */
    @Override
    @org.springframework.scheduling.annotation.Async
    public void envoyer(String destinataire, String sujet, String corps, String lienSensible) {
        if (!cleBrevo.isEmpty()) {
            envoyerParBrevo(destinataire, sujet, corps);
        } else if (mailSender != null) {
            envoyerParSmtp(destinataire, sujet, corps);
        } else if (journaliserLiens && lienSensible != null) {
            log.warn("[YAY360][DEV] Aucun envoi d'email configuré — email non envoyé. Lien ou code : {}", lienSensible);
        } else {
            log.info("[YAY360] Aucun envoi d'email configuré (BREVO_API_KEY ou SMTP_HOST) — email non envoyé.");
        }
    }

    void envoyerParBrevo(String destinataire, String sujet, String corps) {
        try {
            brevo.post()
                    .uri(URL_BREVO)
                    .header("api-key", cleBrevo)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "sender", Map.of("email", expediteur, "name", expediteurNom),
                            "to", List.of(Map.of("email", destinataire)),
                            "subject", sujet,
                            "textContent", corps))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            // Ni le destinataire ni le contenu (code, lien) ne sont journalisés
            log.error("[YAY360] Échec de l'envoi d'un email par Brevo : {}", e.getClass().getSimpleName());
        }
    }

    private void envoyerParSmtp(String destinataire, String sujet, String corps) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(expediteur);
            message.setTo(destinataire);
            message.setSubject(sujet);
            message.setText(corps);
            mailSender.send(message);
        } catch (MailException e) {
            log.error("[YAY360] Échec de l'envoi d'un email : {}", e.getClass().getSimpleName());
        }
    }
}
