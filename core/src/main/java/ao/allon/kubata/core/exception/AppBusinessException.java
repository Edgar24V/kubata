package ao.allon.kubata.core.exception;

/**
 * Exceção base para erros de negócio que devem ser exibidos ao usuário.
 */
public class AppBusinessException extends RuntimeException {
    
    private final String code;

    public AppBusinessException(String message) {
        super(message);
        this.code = "BUSINESS_ERROR";
    }

    public AppBusinessException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
