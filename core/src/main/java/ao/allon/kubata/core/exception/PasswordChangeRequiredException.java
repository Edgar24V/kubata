package ao.allon.kubata.core.exception;

import ao.allon.kubata.core.domain.User;

/**
 * Indica que as credenciais foram validadas, mas o acesso normal está bloqueado
 * até que a palavra-passe seja substituída. Acontece com uma palavra-passe
 * provisória (primeiro acesso) ou com uma palavra-passe que já expirou.
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

    /** Alteração obrigatória porque a palavra-passe actual expirou. */
    public static PasswordChangeRequiredException expired(User user) {
        return new PasswordChangeRequiredException(user, true);
    }

    public User getUser() {
        return user;
    }

    /** {@code true} quando a alteração é exigida por expiração e não por ser provisória. */
    public boolean isExpired() {
        return expired;
    }
}
