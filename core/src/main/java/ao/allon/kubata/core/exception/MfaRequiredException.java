package ao.allon.kubata.core.exception;

/**
 * Indica que a palavra-passe foi validada mas a conta exige o código de segundo
 * fator antes de prosseguir. A view usa-a para revelar o campo do código, pelo
 * que não deve ser tratada como uma credencial inválida.
 */
public class MfaRequiredException extends AuthenticationException {

    public MfaRequiredException() {
        super("Esta conta exige o código MFA.");
    }
}
