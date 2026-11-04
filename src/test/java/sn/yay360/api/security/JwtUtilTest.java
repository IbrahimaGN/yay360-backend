package sn.yay360.api.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private static final String SECRET = "un-secret-de-test-suffisamment-long-pour-hs256-0123456789";

    private JwtUtil jwtUtil(String secret, long expirationMs) {
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "secret", secret);
        ReflectionTestUtils.setField(util, "expiration", expirationMs);
        util.initialiserCle();
        return util;
    }

    @Test
    void refuseDeDemarrerSansSecret() {
        assertThatThrownBy(() -> jwtUtil("", 1000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void refuseDeDemarrerAvecUnSecretTropCourt() {
        assertThatThrownBy(() -> jwtUtil("trop-court", 1000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 octets");
    }

    @Test
    void genereUnJetonDontOnRetrouveLesInformations() {
        JwtUtil util = jwtUtil(SECRET, 60_000);

        String jeton = util.genererToken(12L, "awa@yay360.sn", "FEMME_ENCEINTE");

        assertThat(util.extraireEmail(jeton)).isEqualTo("awa@yay360.sn");
        assertThat(util.extraireUserId(jeton)).isEqualTo(12L);
        assertThat(util.extraireRole(jeton)).isEqualTo("FEMME_ENCEINTE");
        assertThat(util.extraireDateEmission(jeton)).isNotNull();
        assertThat(util.estValide(jeton, "awa@yay360.sn")).isTrue();
        assertThat(util.estValide(jeton, "autre@yay360.sn")).isFalse();
    }

    @Test
    void porteLaVersionDuJetonPourLaRevocation() {
        JwtUtil util = jwtUtil(SECRET, 60_000);

        assertThat(util.extraireVersion(util.genererToken(1L, "a@yay360.sn", "ADMIN", 3))).isEqualTo(3);
        assertThat(util.extraireVersion(util.genererToken(1L, "a@yay360.sn", "ADMIN"))).isZero();
    }

    @Test
    void refuseUnJetonSigneAvecUneAutreCle() {
        String jetonFalsifie = jwtUtil("une-autre-cle-de-signature-tout-aussi-longue-0123456789", 60_000)
                .genererToken(1L, "admin@yay360.sn", "ADMIN");

        assertThatThrownBy(() -> jwtUtil(SECRET, 60_000).extraireEmail(jetonFalsifie))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void refuseUnJetonExpire() {
        JwtUtil util = jwtUtil(SECRET, -1_000);
        String jeton = util.genererToken(1L, "awa@yay360.sn", "FEMME_ENCEINTE");

        assertThatThrownBy(() -> util.extraireEmail(jeton)).isInstanceOf(ExpiredJwtException.class);
    }
}
