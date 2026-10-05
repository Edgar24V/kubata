package ao.allon.kubata.core.exception;

/**
 * Indica que a conta atingiu o número máximo de sessões simultâneas permitido
 * pela sua política individual de segurança.
 *
 * <p>É uma exceção específica para que as interfaces de login possam explicar
 * o bloqueio ao utilizador sem depender da comparação de mensagens.</p>
 */
public class SessionLimitExceededException extends AuthenticationException {

    private final int maxSessions;
    private final int activeSessions;
    private final boolean administrator;

    public SessionLimitExceededException(
            int maxSessions,
            int activeSessions,
            boolean administrator) {
        super("O limite de sessões simultâneas desta conta foi atingido.");
        this.maxSessions = maxSessions;
        this.activeSessions = activeSessions;
        this.administrator = administrator;
    }

    public SessionLimitExceededException(int maxSessions, int activeSessions) {
        this(maxSessions, activeSessions, false);
    }

    public int getMaxSessions() {
        return maxSessions;
    }

    public int getActiveSessions() {
        return activeSessions;
    }

    public boolean isAdministrator() {
        return administrator;
    }
}
