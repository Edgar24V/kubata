package ao.allon.kubata.core.security;

import ao.allon.kubata.core.domain.PermissaoPerfil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Traduz as verificações históricas (módulo + opção) para a estrutura RBAC
 * definida no Kubata Administrator (módulo + recurso + operação).
 */
public final class PermissionCatalog {

    public record Check(String modulo, String recurso, PermissaoPerfil.Operacao operacao) {
    }

    public static final String ADMINISTRATOR = "ADMINISTRATOR";
    public static final String FATURACAO = "FATURACAO";
    public static final String ESTOQUE = "ESTOQUE";
    public static final String INVENTARIO = "INVENTARIO";
    public static final String VENDAS = "VENDAS";
    public static final String COMPRAS = "COMPRAS";
    public static final String FINANCEIRO = "FINANCEIRO";
    public static final String CONTABILIDADE = "CONTABILIDADE";
    public static final String RH = "RH";
    public static final String FISCAL = "FISCAL";

    private static final Map<String, String> APPLICATION_TO_PERMISSION = Map.of(
            "admin", ADMINISTRATOR,
            "administrator", ADMINISTRATOR,
            "faturacao", FATURACAO,
            "rh", RH,
            "hr", RH
    );

    private static final Map<String, String> CATALOG_CODE_TO_PERMISSION = Map.ofEntries(
            Map.entry("faturacao", FATURACAO),
            Map.entry("inventario", ESTOQUE),
            Map.entry("vendas", VENDAS),
            Map.entry("compras", COMPRAS),
            Map.entry("financeiro", FINANCEIRO),
            Map.entry("contabilidade", CONTABILIDADE),
            Map.entry("fiscal", FISCAL),
            Map.entry("rh", RH),
            Map.entry("admin", ADMINISTRATOR),
            Map.entry("administrator", ADMINISTRATOR)
    );

    /**
     * Códigos que podem representar cada aplicação em adm_modulo_sistema:
     * o id do módulo registado no Kubata Administrator e os códigos legados
     * semeados na migração V10. A ativação/desativação feita no Administrator
     * em qualquer um destes códigos bloqueia a aplicação em todos os módulos.
     */
    private static final Map<String, Set<String>> PERMISSION_TO_CATALOG_CODES = Map.of(
            FATURACAO, Set.of("faturacao", "billing"),
            RH, Set.of("rh", "hr"),
            ESTOQUE, Set.of("inventario", "estoque", "stock"),
            VENDAS, Set.of("vendas"),
            COMPRAS, Set.of("compras"),
            FINANCEIRO, Set.of("financeiro"),
            CONTABILIDADE, Set.of("contabilidade"),
            FISCAL, Set.of("fiscal"),
            ADMINISTRATOR, Set.of("administrator", "admin")
    );

    /**
     * Códigos equivalentes na lista de módulos permitidos do perfil de segurança:
     * o editor de perfis escreve INVENTARIO e o catálogo de permissões usa ESTOQUE,
     * pelo que uma comparação exacta trancaria utilizadores legitimamente autorizados.
     */
    private static final Map<String, Set<String>> PERMISSION_MODULE_ALIASES = Map.of(
            ESTOQUE, Set.of(ESTOQUE, INVENTARIO, "STOCK"),
            INVENTARIO, Set.of(ESTOQUE, INVENTARIO, "STOCK"),
            ADMINISTRATOR, Set.of(ADMINISTRATOR, "ADMIN"),
            FATURACAO, Set.of(FATURACAO, "BILLING"),
            RH, Set.of(RH, "HR")
    );

    private PermissionCatalog() {
    }


    public static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    public static String permissionModuleForApplication(String applicationCode) {
        if (applicationCode == null || applicationCode.isBlank()) {
            return null;
        }
        String key = applicationCode.trim().toLowerCase(Locale.ROOT);
        return APPLICATION_TO_PERMISSION.getOrDefault(key, normalize(applicationCode));
    }

    public static String catalogCodeForApplication(String applicationCode) {
        String permission = permissionModuleForApplication(applicationCode);
        if (FATURACAO.equals(permission)) {
            return "faturacao";
        }
        if (RH.equals(permission)) {
            return "rh";
        }
        if (ADMINISTRATOR.equals(permission)) {
            return "administrator";
        }
        return permission == null ? null : permission.toLowerCase(Locale.ROOT);
    }

    public static String permissionModuleForCatalog(String catalogCode) {
        if (catalogCode == null || catalogCode.isBlank()) {
            return null;
        }
        return CATALOG_CODE_TO_PERMISSION.getOrDefault(
                catalogCode.trim().toLowerCase(Locale.ROOT),
                normalize(catalogCode)
        );
    }

    /**
     * Todos os códigos de catálogo que podem controlar o acesso à aplicação
     * correspondente ao módulo de permissões indicado (ex.: FATURACAO
     * -&gt; "faturacao" e "billing").
     */
    public static Set<String> catalogCodesForPermission(String permissionModule) {
        String key = normalize(permissionModule);
        if (key.isEmpty()) {
            return Set.of();
        }
        return PERMISSION_TO_CATALOG_CODES.getOrDefault(key, Set.of(key.toLowerCase(Locale.ROOT)));
    }

    /**
     * Variantes do módulo de permissões usadas no armazenamento dos perfis.
     */
    public static Set<String> moduleAliases(String permissionModule) {
        String key = normalize(permissionModule);
        if (key.isEmpty()) {
            return Set.of();
        }
        return PERMISSION_MODULE_ALIASES.getOrDefault(key, Set.of(key));
    }

    /**
     * Módulo usado no perfil de segurança individual (lista de módulos permitidos).
     */
    public static String canonicalSecurityModule(String modulo) {
        String key = normalize(modulo);
        return switch (key) {
            case "CLIENTES", "RECIBOS", "PDV", "AGT", "SAFT", "SERIES",
                 "MOTIVOS_ISENCAO", "RETENCAO", "IMPOSTOS", "LOGISTICA",
                 "PROFORMAS", "ORCAMENTOS" -> FATURACAO;
            case "PRODUTOS", "ARTIGOS", "ARMAZENS", INVENTARIO -> ESTOQUE;
            case "FORNECEDORES", "ENCOMENDAS" -> COMPRAS;
            case "CAIXA", "BANCOS", "PAGAMENTOS" -> FINANCEIRO;
            case "CONFIG", "CONFIGURACOES", "PARAMETROS", "EMPRESA" -> ADMINISTRATOR;
            case "RELATORIOS" -> FATURACAO;
            default -> key;
        };
    }

    public static Set<String> entryModulesForApplication(String applicationCode) {
        String permission = permissionModuleForApplication(applicationCode);
        if (FATURACAO.equals(permission)) {
            return Set.of(FATURACAO, ESTOQUE, VENDAS, COMPRAS, FINANCEIRO, CONTABILIDADE, FISCAL);
        }
        if (permission == null || permission.isBlank()) {
            return Set.of();
        }
        return Set.of(permission);
    }

    public static List<Check> resolve(String modulo, String opcao) {
        String moduleKey = normalize(modulo);
        String optionKey = normalize(opcao);
        List<Check> checks = new ArrayList<>();

        switch (moduleKey) {
            case "FATURACAO" -> addFaturacao(checks, optionKey);
            case "CLIENTES" -> addCrud(checks, FATURACAO, "CLIENTES", optionKey);
            case "RECIBOS" -> addFaturacao(checks, optionKey);
            case "ESTOQUE", "INVENTARIO", "PRODUTOS" -> addEstoque(checks, optionKey);
            case "FORNECEDORES" -> addCrud(checks, COMPRAS, "FORNECEDORES", optionKey);
            case "COMPRAS" -> addCrud(checks, COMPRAS, "ENCOMENDAS", optionKey);
            case "PDV", "CAIXA" -> addFinanceiroCaixa(checks, optionKey);
            case "RELATORIOS" -> addRelatorios(checks, optionKey);
            case "SAFT", "AGT", "IMPOSTOS", "MOTIVOS_ISENCAO", "RETENCAO" ->
                    addFiscal(checks, optionKey);
            case "SERIES" -> add(checks, ADMINISTRATOR, "SERIES", toOperacao(optionKey));
            case "CONFIG", "CONFIGURACOES" -> addConfig(checks, optionKey);
            case "CONTABILIDADE" -> addCrud(checks, CONTABILIDADE, "LANCAMENTOS", optionKey);
            case "LOGISTICA" -> add(checks, VENDAS, "DOCUMENTOS", toOperacao(optionKey));
            case "RH" -> addCrud(checks, RH, "FUNCIONARIOS", optionKey);
            case "VENDAS" -> addCrud(checks, VENDAS, "DOCUMENTOS", optionKey);
            case "FINANCEIRO" -> addFinanceiroCaixa(checks, optionKey);
            case "ADMINISTRATOR" -> add(checks, ADMINISTRATOR, optionKey.isBlank() ? "UTILIZADORES" : optionKey,
                    PermissaoPerfil.Operacao.VER);
            default -> {
                add(checks, moduleKey, optionKey, toOperacao(optionKey));
                add(checks, moduleKey, moduleKey, toOperacao(optionKey));
            }
        }

        return checks.stream().distinct().toList();
    }

    private static void addFaturacao(List<Check> checks, String optionKey) {
        PermissaoPerfil.Operacao operacao = toOperacao(optionKey);
        add(checks, FATURACAO, "FATURAS", operacao);
        if (operacao == PermissaoPerfil.Operacao.VER) {
            add(checks, FATURACAO, "NOTAS_CREDITO", PermissaoPerfil.Operacao.VER);
        }
        if (operacao == PermissaoPerfil.Operacao.ANULAR) {
            add(checks, FATURACAO, "NOTAS_CREDITO", PermissaoPerfil.Operacao.ANULAR);
        }
    }

    private static void addEstoque(List<Check> checks, String optionKey) {
        PermissaoPerfil.Operacao operacao = toOperacao(optionKey);
        if ("TRANSFERIR".equals(optionKey)) {
            add(checks, ESTOQUE, "ARMAZENS", PermissaoPerfil.Operacao.EDITAR);
            add(checks, ESTOQUE, "MOVIMENTOS", PermissaoPerfil.Operacao.CRIAR);
            return;
        }
        add(checks, ESTOQUE, "ARTIGOS", operacao);
        add(checks, ESTOQUE, "MOVIMENTOS", operacao);
        add(checks, ESTOQUE, "ARMAZENS", operacao);
    }

    private static void addFinanceiroCaixa(List<Check> checks, String optionKey) {
        PermissaoPerfil.Operacao operacao = toOperacao(optionKey);
        if ("ABRIR".equals(optionKey)) {
            add(checks, FINANCEIRO, "CAIXA", PermissaoPerfil.Operacao.VER);
            add(checks, FATURACAO, "FATURAS", PermissaoPerfil.Operacao.CRIAR);
            return;
        }
        add(checks, FINANCEIRO, "CAIXA", operacao);
        add(checks, FINANCEIRO, "BANCOS", operacao);
        add(checks, FINANCEIRO, "PAGAMENTOS", operacao);
    }

    private static void addRelatorios(List<Check> checks, String optionKey) {
        if ("FINANCEIRO".equals(optionKey)) {
            add(checks, FINANCEIRO, "CAIXA", PermissaoPerfil.Operacao.VER);
            add(checks, CONTABILIDADE, "LANCAMENTOS", PermissaoPerfil.Operacao.VER);
            return;
        }
        add(checks, VENDAS, "DOCUMENTOS", PermissaoPerfil.Operacao.VER);
        add(checks, FATURACAO, "FATURAS", PermissaoPerfil.Operacao.VER);
    }

    private static void addFiscal(List<Check> checks, String optionKey) {
        PermissaoPerfil.Operacao operacao = toOperacao(optionKey);
        add(checks, ADMINISTRATOR, "FISCAL_AGT", operacao);
        add(checks, FATURACAO, "FATURAS",
                operacao == PermissaoPerfil.Operacao.EDITAR
                        ? PermissaoPerfil.Operacao.EXPORTAR
                        : operacao);
    }

    private static void addConfig(List<Check> checks, String optionKey) {
        PermissaoPerfil.Operacao operacao = toOperacao(optionKey);
        add(checks, ADMINISTRATOR, "EMPRESAS", operacao);
        add(checks, ADMINISTRATOR, "PARAMETROS", operacao);
    }

    private static void addCrud(
            List<Check> checks,
            String modulo,
            String recurso,
            String optionKey
    ) {
        add(checks, modulo, recurso, toOperacao(optionKey));
    }

    private static void add(
            List<Check> checks,
            String modulo,
            String recurso,
            PermissaoPerfil.Operacao operacao
    ) {
        if (modulo == null || modulo.isBlank() || recurso == null || recurso.isBlank() || operacao == null) {
            return;
        }
        checks.add(new Check(modulo, recurso, operacao));
    }

    static PermissaoPerfil.Operacao toOperacao(String optionKey) {
        return switch (optionKey) {
            case "CRIAR", "EMITIR", "ABRIR", "GERIR", "TABELAS" -> PermissaoPerfil.Operacao.CRIAR;
            case "EDITAR", "TRANSFERIR", "SUBMETER", "BALANCA",
                 "EDITAR QUANTIDADE", "EDITAR DESCONTO", "EDITAR PRECO",
                 "EDITAR PREÇO" -> PermissaoPerfil.Operacao.EDITAR;
            case "APAGAR", "EXCLUIR", "REMOVER" -> PermissaoPerfil.Operacao.APAGAR;
            case "ANULAR" -> PermissaoPerfil.Operacao.ANULAR;
            case "IMPRIMIR" -> PermissaoPerfil.Operacao.IMPRIMIR;
            case "EXPORTAR", "MAPAS" -> PermissaoPerfil.Operacao.EXPORTAR;
            case "APROVAR" -> PermissaoPerfil.Operacao.APROVAR;
            default -> PermissaoPerfil.Operacao.VER;
        };
    }
}
