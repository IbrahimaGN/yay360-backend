package sn.yay360.api.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EmailServiceImplTest {

    @SuppressWarnings("unchecked")
    private final ObjectProvider<JavaMailSender> pasDeSmtp = mock(ObjectProvider.class);

    @Test
    void avecUneCleBrevo_envoieParLApiHttps() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer brevo = MockRestServiceServer.bindTo(builder).build();
        brevo.expect(requestTo(EmailServiceImpl.URL_BREVO))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "cle-de-test"))
                .andExpect(jsonPath("$.sender.email").value("no-reply@yay360.sn"))
                .andExpect(jsonPath("$.sender.name").value("YAY360"))
                .andExpect(jsonPath("$.to[0].email").value("awa@exemple.sn"))
                .andExpect(jsonPath("$.subject").value("Sujet"))
                .andExpect(jsonPath("$.textContent").value("Corps"))
                .andRespond(withSuccess("{\"messageId\":\"<id@brevo>\"}", org.springframework.http.MediaType.APPLICATION_JSON));
        EmailServiceImpl service = new EmailServiceImpl(pasDeSmtp, builder, "", "cle-de-test",
                "no-reply@yay360.sn", "YAY360", false);

        service.envoyer("awa@exemple.sn", "Sujet", "Corps", null);

        assertThat(service.estConfigure()).isTrue();
        brevo.verify();
    }

    @Test
    void erreurBrevo_nInterromptPasLOperation() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer brevo = MockRestServiceServer.bindTo(builder).build();
        brevo.expect(requestTo(EmailServiceImpl.URL_BREVO)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        EmailServiceImpl service = new EmailServiceImpl(pasDeSmtp, builder, "", "mauvaise-cle",
                "no-reply@yay360.sn", "YAY360", false);

        service.envoyer("awa@exemple.sn", "Sujet", "Corps", null); // aucune exception

        brevo.verify();
    }

    @Test
    void sansBrevoNiSmtp_nonConfigure() {
        EmailServiceImpl service = new EmailServiceImpl(pasDeSmtp, RestClient.builder(), "", "",
                "no-reply@yay360.sn", "YAY360", false);

        assertThat(service.estConfigure()).isFalse();
        service.envoyer("awa@exemple.sn", "Sujet", "Corps", "123456"); // rien n'est envoyé
    }
}
