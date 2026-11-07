package sn.yay360.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    /** HS256 exige une clé d'au moins 256 bits. */
    static final int LONGUEUR_MINIMALE_SECRET = 32;

    @Value("${jwt.secret:}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey signingKey;

    /**
     * Construit la clé une seule fois au démarrage et refuse de démarrer
     * si le secret est absent ou trop faible (plutôt que d'échouer au premier login).
     */
    @PostConstruct
    void initialiserCle() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET n'est pas défini. Renseignez-le dans le fichier .env (voir .env.example).");
        }
        byte[] octets = secret.getBytes(StandardCharsets.UTF_8);
        if (octets.length < LONGUEUR_MINIMALE_SECRET) {
            throw new IllegalStateException(
                    "JWT_SECRET est trop court : " + LONGUEUR_MINIMALE_SECRET + " octets minimum requis.");
        }
        signingKey = Keys.hmacShaKeyFor(octets);
    }

    private SecretKey getSigningKey() {
        return signingKey;
    }

    public String genererToken(Long userId, String email, String role) {
        return genererToken(userId, email, role, 0);
    }

    public String genererToken(Long userId, String email, String role, Integer versionJeton) {
        Date maintenant = new Date();
        Date expirationDate = new Date(maintenant.getTime() + expiration);

        return Jwts.builder()
                .subject(email)
                .claim("id", userId)
                .claim("role", role)
                .claim("ver", versionJeton == null ? 0 : versionJeton)
                .issuedAt(maintenant)
                .expiration(expirationDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String extraireEmail(String token) {
        return extraireClaim(token, Claims::getSubject);
    }

    public Long extraireUserId(String token) {
        Claims claims = extraireToutesLesClaims(token);
        return claims.get("id", Long.class);
    }

    public String extraireRole(String token) {
        Claims claims = extraireToutesLesClaims(token);
        return claims.get("role", String.class);
    }

    public int extraireVersion(String token) {
        Integer version = extraireToutesLesClaims(token).get("ver", Integer.class);
        return version == null ? 0 : version;
    }

    public Date extraireDateEmission(String token) {
        return extraireClaim(token, Claims::getIssuedAt);
    }

    public Date extraireDateExpiration(String token) {
        return extraireClaim(token, Claims::getExpiration);
    }

    public boolean estExpire(String token) {
        return extraireDateExpiration(token).before(new Date());
    }

    public boolean estValide(String token, String email) {
        String emailDuToken = extraireEmail(token);
        return emailDuToken.equals(email) && !estExpire(token);
    }

    private <T> T extraireClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extraireToutesLesClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extraireToutesLesClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
