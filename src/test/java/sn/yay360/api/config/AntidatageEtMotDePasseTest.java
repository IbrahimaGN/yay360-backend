package sn.yay360.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import sn.yay360.api.helper.MoteurReglesCliniques;
import sn.yay360.api.repo.AlerteMedicaleRepository;
import sn.yay360.api.repo.ConsultationRepository;
import sn.yay360.api.repo.EtablissementSanteRepository;
import sn.yay360.api.repo.GrossesseRepository;
import sn.yay360.api.repo.NotificationRepository;
import sn.yay360.api.repo.RendezVousRepository;
import sn.yay360.api.repo.UtilisateurRepository;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Requêtes d'antidatage figées (pas de SQL assemblé) et mot de passe administrateur généré. */
class AntidatageEtMotDePasseTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final JeuDeDonneesDemo jeu = new JeuDeDonneesDemo(mock(UtilisateurRepository.class),
            mock(EtablissementSanteRepository.class), mock(GrossesseRepository.class),
            mock(RendezVousRepository.class), mock(ConsultationRepository.class),
            mock(AlerteMedicaleRepository.class), mock(NotificationRepository.class),
            mock(PasswordEncoder.class), new MoteurReglesCliniques(), jdbcTemplate);
    private final LocalDateTime date = LocalDateTime.of(2026, 11, 1, 10, 0);

    @Test
    void antidaterUtiliseUneRequeteFixePourChaqueColonneAutorisee() {
        jeu.antidater("notifications", "date_envoi", 7L, date);
        jeu.antidater("alertes_medicales", "date_declaration", 8L, date);

        verify(jdbcTemplate).update("UPDATE notifications SET date_envoi = ? WHERE id = ?", Timestamp.valueOf(date), 7L);
        verify(jdbcTemplate).update("UPDATE alertes_medicales SET date_declaration = ? WHERE id = ?",
                Timestamp.valueOf(date), 8L);
    }

    @Test
    void antidaterRefuseUneColonneNonPrevue_etIgnoreUnIdAbsent() {
        assertThatThrownBy(() -> jeu.antidater("utilisateurs", "role", 1L, date))
                .isInstanceOf(IllegalArgumentException.class);
        jeu.antidater("notifications", "date_envoi", null, date);

        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void leMotDePasseGenereEstRobusteEtDifferentAChaqueAppel() {
        String premier = DataInitializer.genererMotDePasse();
        String second = DataInitializer.genererMotDePasse();

        assertThat(premier).startsWith("Yay-").endsWith("9").hasSize(21);
        assertThat(premier).isNotEqualTo(second);
    }
}
