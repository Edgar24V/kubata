package ao.allon.kubata.core.exception;

public class AuthenticationException extends AppBusinessException {
    public AuthenticationException(String message) {
        super(message, "AUTH_ERROR");
    }
}
