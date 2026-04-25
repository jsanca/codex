package codex.fundamentum.api.exception;

public class InvalidStateTransitionException extends RuntimeException{

    public InvalidStateTransitionException() {
    }

    public InvalidStateTransitionException(String message) {
        super(message);
    }

    public InvalidStateTransitionException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidStateTransitionException(Throwable cause) {
        super(cause);
    }

    public InvalidStateTransitionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
