package sn.yay360.api.exception;

import sn.yay360.api.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gestionnaire global des exceptions : format d'erreur uniforme sur toute l'API.
 * Les erreurs du client reçoivent un code 4xx précis ; seules les erreurs réellement
 * inattendues donnent un 500, toujours journalisé avec sa pile pour le diagnostic.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================== 4xx MÉTIER ====================

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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Requête invalide", ex.getMessage(), request, null);
    }

    // ==================== 4xx REQUÊTE MAL FORMÉE ====================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> champsInvalides = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            champsInvalides.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "Erreur de validation", "Les données envoyées sont invalides", request, champsInvalides);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Erreur de validation", ex.getMessage(), request, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Requête illisible",
                "Le corps de la requête est absent ou mal formé (JSON, format de date…)", request, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Paramètre manquant",
                "Le paramètre « " + ex.getParameterName() + " » est obligatoire", request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Paramètre invalide",
                "La valeur du paramètre « " + ex.getName() + " » est invalide", request, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Ressource introuvable", "Cette adresse n'existe pas", request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Méthode non autorisée",
                "La méthode " + ex.getMethod() + " n'est pas prise en charge sur cette adresse", request, null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Format non pris en charge",
                "Envoyez les données au format application/json", request, null);
    }

    // ==================== 409 CONFLITS DE DONNÉES ====================

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        // Uniquement le nom de la contrainte : le détail PostgreSQL contient les valeurs (emails…)
        String contrainte = ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException c
                ? c.getConstraintName() : "inconnue";
        log.warn("[YAY360] Violation d'intégrité sur {} (contrainte : {})", request.getRequestURI(), contrainte);
        return build(HttpStatus.CONFLICT, "Conflit de données",
                "Opération impossible : ces données sont liées à d'autres enregistrements ou existent déjà. "
                        + "Pour un compte, préférez la désactivation à la suppression.", request, null);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(OptimisticLockingFailureException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Conflit de modification",
                "Ces données ont été modifiées entre-temps. Rechargez puis réessayez.", request, null);
    }

    // ==================== 500 ====================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("[YAY360] Erreur inattendue sur {} {}", request.getMethod(), request.getRequestURI(), ex);
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
