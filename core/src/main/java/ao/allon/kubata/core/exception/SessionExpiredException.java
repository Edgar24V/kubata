package ao.allon.kubata.core.exception;

/**
 * A sessão do utilizador ultrapassou o tempo limite do perfil de segurança. A operação é
 * interrompida e a aplicação deve devolver o utilizador ao ecrã de credenciais.
 */
public class SessionExpiredException extends AppBusinessException {
    public SessionExpiredException(String message) {
        super(message, "SESSION_EXPIRED");
    }
}
