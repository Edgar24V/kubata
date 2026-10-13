package ao.allon.kubata.core.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModuleAccessRulesTest {

    @Test
    void deveReconhecerOsAliasesDeInventarioEVendas() {
        assertTrue(ModuleAccessRules.moduleMatches("STOCK", "inventario"));
        assertTrue(ModuleAccessRules.moduleMatches("INVENTORY", "Inventário"));
        assertTrue(ModuleAccessRules.moduleMatches("FATURACAO", "vendas"));
        assertTrue(ModuleAccessRules.moduleMatches("BILLING", "VENDAS"));
        assertFalse(ModuleAccessRules.moduleMatches("STOCK", "financeiro"));
    }

    @Test
    void deveNormalizarAcentosEspacosEOperacoesLegadas() {
        assertTrue(ModuleAccessRules.operationMatches("Ver", "VER"));
        assertTrue(ModuleAccessRules.operationMatches("Visualizar", "VER"));
        assertTrue(ModuleAccessRules.operationMatches("Criar", "CRIAR"));
        assertTrue(ModuleAccessRules.operationMatches("Emitir", "CRIAR"));
        assertTrue(ModuleAccessRules.operationMatches("Remover", "APAGAR"));
        assertTrue(ModuleAccessRules.operationMatches("Alterar Preço Margem", "ALTERAR_PRECO_MARGEM"));
        assertFalse(ModuleAccessRules.operationMatches("Anular", "APAGAR"));
    }

    @Test
    void codigosDesconhecidosContinuamExigindoCorrespondenciaExacta() {
        assertTrue(ModuleAccessRules.moduleMatches("MODULO_X", "modulo_x"));
        assertFalse(ModuleAccessRules.moduleMatches("MODULO_X", "MODULO_Y"));
    }
}
