package sn.yay360.api.exception;

import sn.yay360.api.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire global des exceptions.
 * Garantit un format de réponse d'erreur uniforme sur toute l'API,
 * conformément à l'exigence "gérer les erreurs de manière uniforme".
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Ressource introuvable", ex.getMessage(), request, null);
    }

    @ExceptionHandler(EmailDejaUtiliseException.class)
    public ResponseEntity<ErrorResponse> handleEmailDejaUtilise(EmailDejaUtiliseException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Conflit", ex.getMessage(), request, null);
    }

    @ExceptionHandler(AuthentificationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentification(AuthentificationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Échec de l'authentification", ex.getMessage(), request, null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Échec de l'authentification", "Email ou mot de passe incorrect", request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Accès refusé", "Vous n'avez pas les droits nécessaires pour cette opération", request, null);
    }

    @ExceptionHandler(OperationNonAutoriseeException.class)
    public ResponseEntity<ErrorResponse> handleOperationNonAutorisee(OperationNonAutoriseeException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Opération non autorisée", ex.getMessage(), request, null);
    }

    @ExceptionHandler(AssistantIndisponibleException.class)
    public ResponseEntity<ErrorResponse> handleAssistantIndisponible(AssistantIndisponibleException ex,
                                                                      HttpServletRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Assistant IA indisponible", ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> champsInvalides = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            champsInvalides.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "Erreur de validation", "Les données envoyées sont invalides", request, champsInvalides);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Requête invalide", ex.getMessage(), request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne", "Une erreur inattendue est survenue", request, null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String erreur, String message,
                                                 HttpServletRequest request, Map<String, String> champsInvalides) {
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                erreur,
                message,
                request.getRequestURI(),
                champsInvalides
        );
        return ResponseEntity.status(status).body(body);
    }
}
