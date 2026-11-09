package sn.yay360.api.exception;

/** Trop de demandes en peu de temps (envoi de codes) : réponse HTTP 429. */
public class TropDeDemandesException extends RuntimeException {

    public TropDeDemandesException(String message) {
        super(message);
    }
}
