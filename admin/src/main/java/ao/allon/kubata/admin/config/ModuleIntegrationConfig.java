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
        
        // Preparar o catálogo funcional uma vez no registo. A navegação não
        // deve reinicializar um módulo partilhado sempre que abre uma página.
        List<KubataModule> modules = List.of(
            inventarioModule,
            vendasModule,
            comprasModule,
            financeiroModule,
            contabilidadeModule,
            fiscalModule
        );

        modules.forEach(module -> {
            module.initialize();
            moduleRegistry.registerModule(module);
        });

        return modules;
    }
}
