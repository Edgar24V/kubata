package ao.allon.kubata.admin.ui;

/**
 * Contrato simples para vistas que conseguem indicar se possuem
 * alterações locais ainda não persistidas.
 */
@FunctionalInterface
public interface UnsavedChangesAware {

    boolean hasUnsavedChanges();

    default String getUnsavedChangesMessage() {
        return "Existem alterações por guardar nesta área.";
    }
}
