package ao.allon.kubata.core.exception;

import ao.allon.kubata.core.domain.User;

/**
 * Indica que as credenciais foram validadas, mas o acesso normal está bloqueado
 * até que uma palavra-passe provisória seja substituída.
 */
public class PasswordChangeRequiredException extends AuthenticationException {

    private final User user;

    public PasswordChangeRequiredException(User user) {
        super("A palavra-passe provisória deve ser alterada antes de continuar.");
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
