package ao.allon.kubata.core.exception;

/**
 * Operação recusada porque o perfil do utilizador não a permite. Lançada ao nível do serviço,
 * nunca só pelo menu, para que uma chamada directa ao código Produza o mesmo resultado.
 */
public class ForbiddenActionException extends AppBusinessException {
    public ForbiddenActionException(String message) {
        super(message, "FORBIDDEN");
    }
}
