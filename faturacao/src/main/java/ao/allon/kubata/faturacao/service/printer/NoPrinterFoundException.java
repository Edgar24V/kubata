package ao.allon.kubata.faturacao.service.printer;

public class NoPrinterFoundException extends RuntimeException {
    public NoPrinterFoundException(String message) { super(message); }
}

