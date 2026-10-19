package sn.yay360.api.exception;

public class AssistantIndisponibleException extends RuntimeException {

    public AssistantIndisponibleException(String message) {
        super(message);
    }

    public AssistantIndisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
