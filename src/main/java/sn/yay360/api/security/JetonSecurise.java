package sn.yay360.api.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Génération et hachage des jetons à usage unique (réinitialisation de mot de passe,
 * invitation du conjoint). Seule l'empreinte SHA-256 est stockée en base : une fuite
 * de la base ne permet donc pas d'utiliser les jetons en cours de validité.
 */
public final class JetonSecurise {

    private static final SecureRandom ALEATOIRE = new SecureRandom();

    private JetonSecurise() {
    }

    /** Jeton aléatoire de 256 bits, encodé en base64 URL (43 caractères). */
    public static String generer() {
        byte[] octets = new byte[32];
        ALEATOIRE.nextBytes(octets);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
    }

    /** Empreinte SHA-256 hexadécimale (64 caractères) à stocker en base. */
    public static String hacher(String jeton) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha256.digest(jeton.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
