package ao.allon.kubata.core.module.event;

/**
 * Interface para listeners de eventos de módulos.
 */
@FunctionalInterface
public interface ModuleEventListener {
    
    /**
     * Chamado quando um evento de módulo é disparado.
     * @param event o evento recebido
     */
    void onModuleEvent(ModuleEvent event);
    
    /**
     * Verifica se este listener aceita o tipo de evento especificado.
     * @param eventType o tipo de evento
     * @return true se o listener aceita o evento
     */
    default boolean accepts(String eventType) {
        return true;
    }
}
