package sn.yay360.api.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import sn.yay360.api.service.EmailService;

/**
 * Envoi via SMTP lorsque {@code spring.mail.host} est configuré.
 * Sans SMTP, l'email n'est pas envoyé ; en développement uniquement, le lien peut être
 * journalisé pour permettre les tests ({@code YAY360_MAIL_JOURNALISER_LIENS=true}).
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final String expediteur;
    private final boolean journaliserLiens;

    public EmailServiceImpl(ObjectProvider<JavaMailSender> mailSender,
                            @Value("${spring.mail.host:}") String hoteSmtp,
                            @Value("${yay360.mail.from:no-reply@yay360.sn}") String expediteur,
                            @Value("${yay360.mail.journaliser-liens:false}") boolean journaliserLiens) {
        // Un hôte vide (variable non renseignée) équivaut à « pas de SMTP ».
        this.mailSender = hoteSmtp == null || hoteSmtp.isBlank() ? null : mailSender.getIfAvailable();
        this.expediteur = expediteur;
        this.journaliserLiens = journaliserLiens;
    }

    @Override
    public boolean estConfigure() {
        return mailSender != null;
    }

    /** Asynchrone : le temps de réponse ne révèle pas si un email a été envoyé (pas d'énumération). */
    @Override
    @org.springframework.scheduling.annotation.Async
    public void envoyer(String destinataire, String sujet, String corps, String lienSensible) {
        if (mailSender == null) {
            if (journaliserLiens && lienSensible != null) {
                log.warn("[YAY360][DEV] SMTP non configuré — email non envoyé. Lien : {}", lienSensible);
            } else {
                log.info("[YAY360] SMTP non configuré — email non envoyé.");
            }
            return;
        }
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
