package ao.allon.kubata.core.util;

import java.util.Locale;

/**
 * Validação e normalização de NIF utilizada pelo ecossistema Kubata.
 *
 * <p>As regras locais evitam duplicações por formatação e distinguem o
 * formato usado pelo cadastro empresarial do formato alfanumérico que pode
 * ocorrer na identificação fiscal de pessoas singulares.</p>
 */
public final class NifUtils {

    private static final int EMPRESA_MIN_DIGITS = 9;
    private static final int EMPRESA_MAX_DIGITS = 14;
    private static final int SINGULAR_MIN_LENGTH = 8;
    private static final int SINGULAR_MAX_LENGTH = 20;

    private NifUtils() {
    }

    public enum TipoContribuinte {
        PESSOA_COLECTIVA,
        PESSOA_SINGULAR,
        NAO_RESIDENTE
    }

    /**
     * Normaliza um NIF para armazenamento/comparação.
     * Aceita separadores visuais comuns, mas rejeita outros símbolos.
     */
    public static String normalize(String nif) {
        if (nif == null) {
            return "";
        }

        String value = nif.trim().toUpperCase(Locale.ROOT);
        return value.replaceAll("[\\s.\\-\\/]", "");
    }

    /**
     * Valida o NIF de acordo com o tipo de contribuinte seleccionado no
     * Administrator.
     *
     * <p>Estas são regras de consistência local do software; a atribuição e
     * confirmação oficial do NIF continuam a ser da AGT.</p>
     */
    public static String validate(String nif, TipoContribuinte tipo) {
        String value = normalize(nif);

        if (value.isBlank()) {
            return "NIF é obrigatório.";
        }

        if (tipo == TipoContribuinte.PESSOA_SINGULAR) {
            if (!value.matches("[A-Z0-9]{" + SINGULAR_MIN_LENGTH + "," + SINGULAR_MAX_LENGTH + "}")) {
                return "NIF de pessoa singular inválido: use apenas letras e algarismos, entre "
                        + SINGULAR_MIN_LENGTH + " e " + SINGULAR_MAX_LENGTH + " caracteres.";
            }
            return null;
        }

        if (!value.matches("\\d{" + EMPRESA_MIN_DIGITS + "," + EMPRESA_MAX_DIGITS + "}")) {
            return "NIF de pessoa colectiva/não residente inválido: use apenas algarismos, entre "
                    + EMPRESA_MIN_DIGITS + " e " + EMPRESA_MAX_DIGITS + " caracteres.";
        }

        return null;
    }

    public static boolean isValid(String nif, TipoContribuinte tipo) {
        return validate(nif, tipo) == null;
    }

    public static boolean isValidEmpresaNif(String nif) {
        return isValid(nif, TipoContribuinte.PESSOA_COLECTIVA);
    }
}
