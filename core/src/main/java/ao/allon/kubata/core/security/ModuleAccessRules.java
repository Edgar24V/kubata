package ao.allon.kubata.core.security;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Compatibilidade explícita entre códigos de módulos e operações históricas.
 *
 * O catálogo runtime usa IDs técnicos (por exemplo, "inventario"), enquanto
 * permissões antigas podem usar nomes anteriores (por exemplo, "STOCK").
 * Apenas aliases de negócio definidos neste mapa são tratados como equivalentes.
 */
public final class ModuleAccessRules {

    private static final Map<String, String> MODULE_CANONICAL = Map.ofEntries(
            Map.entry("ADMIN", "ADMINISTRATOR"),
            Map.entry("ADMINISTRATOR", "ADMINISTRATOR"),

            Map.entry("INVENTARIO", "INVENTARIO"),
            Map.entry("INVENTORY", "INVENTARIO"),
            Map.entry("STOCK", "INVENTARIO"),
            Map.entry("STOCKS", "INVENTARIO"),

            Map.entry("VENDAS", "VENDAS"),
            Map.entry("SALES", "VENDAS"),
            Map.entry("FATURACAO", "VENDAS"),
            Map.entry("BILLING", "VENDAS"),

            Map.entry("COMPRAS", "COMPRAS"),
            Map.entry("PURCHASES", "COMPRAS"),
            Map.entry("PURCHASING", "COMPRAS"),

            Map.entry("FINANCEIRO", "FINANCEIRO"),
            Map.entry("FINANCE", "FINANCEIRO"),
            Map.entry("FINANCIAL", "FINANCEIRO"),

            Map.entry("CONTABILIDADE", "CONTABILIDADE"),
            Map.entry("ACCOUNTING", "CONTABILIDADE"),

            Map.entry("FISCAL", "FISCAL"),
            Map.entry("FISCALIDADE", "FISCAL"),
            Map.entry("TAX", "FISCAL")
    );

    private static final Map<String, String> OPERATION_CANONICAL = Map.ofEntries(
            Map.entry("VER", "VER"),
            Map.entry("VIEW", "VER"),
            Map.entry("VISUALIZAR", "VER"),
            Map.entry("CONSULTAR", "VER"),
            Map.entry("LISTAR", "VER"),
            Map.entry("READ", "VER"),

            Map.entry("CRIAR", "CRIAR"),
            Map.entry("CREATE", "CRIAR"),
            Map.entry("NOVO", "CRIAR"),
            Map.entry("NEW", "CRIAR"),
            Map.entry("ADICIONAR", "CRIAR"),
            Map.entry("ADD", "CRIAR"),
            Map.entry("EMITIR", "CRIAR"),
            Map.entry("ISSUE", "CRIAR"),

            Map.entry("EDITAR", "EDITAR"),
            Map.entry("ALTERAR", "EDITAR"),
            Map.entry("MODIFICAR", "EDITAR"),
            Map.entry("UPDATE", "EDITAR"),
            Map.entry("MODIFY", "EDITAR"),

            Map.entry("APAGAR", "APAGAR"),
            Map.entry("REMOVER", "APAGAR"),
            Map.entry("EXCLUIR", "APAGAR"),
            Map.entry("DELETE", "APAGAR"),
            Map.entry("REMOVE", "APAGAR"),

            Map.entry("IMPRIMIR", "IMPRIMIR"),
            Map.entry("PRINT", "IMPRIMIR"),
            Map.entry("EXPORTAR", "EXPORTAR"),
            Map.entry("EXPORT", "EXPORTAR"),
            Map.entry("APROVAR", "APROVAR"),
            Map.entry("APPROVE", "APROVAR"),
            Map.entry("ANULAR", "ANULAR"),
            Map.entry("CANCELAR", "ANULAR"),
            Map.entry("CANCEL", "ANULAR"),
            Map.entry("VOID", "ANULAR")
    );

    private ModuleAccessRules() {
    }

    public static boolean moduleMatches(String grantedModule, String requestedModule) {
        String granted = normalize(grantedModule);
        String requested = normalize(requestedModule);
        if (granted.isBlank() || requested.isBlank()) {
            return false;
        }
        return canonical(MODULE_CANONICAL, granted).equals(canonical(MODULE_CANONICAL, requested));
    }

    public static boolean operationMatches(String grantedOperation, String requestedOperation) {
        String granted = normalize(grantedOperation);
        String requested = normalize(requestedOperation);
        if (granted.isBlank() || requested.isBlank()) {
            return false;
        }
        return canonical(OPERATION_CANONICAL, granted).equals(canonical(OPERATION_CANONICAL, requested));
    }

    private static String canonical(Map<String, String> aliases, String normalizedValue) {
        return aliases.getOrDefault(normalizedValue, normalizedValue);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String withoutAccents = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return withoutAccents
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }
}
