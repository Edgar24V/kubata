package ao.allon.kubata.admin.config;

import ao.allon.kubata.compras.ComprasModule;
import ao.allon.kubata.contabilidade.ContabilidadeModule;
import ao.allon.kubata.core.module.KubataModule;
import ao.allon.kubata.core.module.ModuleRegistry;
import ao.allon.kubata.financeiro.FinanceiroModule;
import ao.allon.kubata.fiscal.FiscalModule;
import ao.allon.kubata.inventario.InventarioModule;
import ao.allon.kubata.vendas.VendasModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.util.List;

/**
 * Configuração de integração dos módulos no admin.
 * Registra todos os módulos do sistema no ModuleRegistry.
 */
@Configuration
public class ModuleIntegrationConfig {
    
    /**
     * Registra todos os módulos Kubata no registry.
     */
    @Bean
    @DependsOn("moduleRegistry")
    public List<KubataModule> registerModules(
            ModuleRegistry moduleRegistry,
            InventarioModule inventarioModule,
            VendasModule vendasModule,
            ComprasModule comprasModule,
            FinanceiroModule financeiroModule,
            ContabilidadeModule contabilidadeModule,
            FiscalModule fiscalModule) {
        
        // Registra todos os módulos
        moduleRegistry.registerModule(inventarioModule);
        moduleRegistry.registerModule(vendasModule);
        moduleRegistry.registerModule(comprasModule);
        moduleRegistry.registerModule(financeiroModule);
        moduleRegistry.registerModule(contabilidadeModule);
        moduleRegistry.registerModule(fiscalModule);
        
        return List.of(
            inventarioModule,
            vendasModule,
            comprasModule,
            financeiroModule,
            contabilidadeModule,
            fiscalModule
        );
    }
}
