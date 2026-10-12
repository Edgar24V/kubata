package ao.allon.kubata.core.exception;

import ao.allon.kubata.core.domain.User;

/**
 * Indica que as credenciais foram validadas, mas o acesso normal está bloqueado
 * até que a palavra-passe seja substituída: por ser provisória ou estar expirada.
 */
public class PasswordChangeRequiredException extends AuthenticationException {

    private final User user;
    private final boolean expired;

    public PasswordChangeRequiredException(User user) {
        super("A palavra-passe provisória deve ser alterada antes de continuar.");
        this.user = user;
        this.expired = false;
    }

    private PasswordChangeRequiredException(User user, boolean expired) {
        super(expired
                ? "A palavra-passe expirou e deve ser alterada antes de continuar."
                : "A palavra-passe provisória deve ser alterada antes de continuar.");
        this.user = user;
        this.expired = expired;
    }

    /** Cria a excepção quando a alteração é exigida por expiração. */
    public static PasswordChangeRequiredException expired(User user) {
        return new PasswordChangeRequiredException(user, true);
    }

    public User getUser() {
        return user;
    }

    /** Indica se a alteração obrigatória resulta da expiração da palavra-passe. */
    public boolean isExpired() {
        return expired;
    }
}
