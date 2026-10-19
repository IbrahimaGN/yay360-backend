package sn.yay360.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String genererToken(Long userId, String email, String role) {
        Date maintenant = new Date();
        Date expirationDate = new Date(maintenant.getTime() + expiration);

        return Jwts.builder()
                .subject(email)
                .claim("id", userId)
                .claim("role", role)
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
