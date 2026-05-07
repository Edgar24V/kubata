package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.core.domain.PlanoConta;
import ao.allon.kubata.core.domain.enums.ClasseConta;
import ao.allon.kubata.core.domain.enums.NaturezaConta;
import ao.allon.kubata.core.repository.PlanoContaRepository;
import ao.allon.kubata.core.service.PlanoContaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Serviço para inicialização do Plano de Contas conforme PGC Angolano (Plano Geral de Contabilidade).
 * Baseado na estrutura oficial do SAF-T-AO e normas contabilísticas angolanas.
 */
@Service
public class PGCAngolanoInitializer {

    private final PlanoContaRepository planoContaRepository;
    private final PlanoContaService planoContaService;

    public PGCAngolanoInitializer(PlanoContaRepository planoContaRepository, 
                                   PlanoContaService planoContaService) {
        this.planoContaRepository = planoContaRepository;
        this.planoContaService = planoContaService;
    }

    /**
     * Inicializa o plano de contas completo do PGC Angolano.
     * Verifica se já existe alguma conta antes de criar.
     */
    @Transactional
    public void inicializarPlanoContasPadrao() {
        long count = planoContaRepository.count();
        if (count > 0) {
            System.out.println("Plano de contas já inicializado com " + count + " contas.");
            return;
        }

        System.out.println("Inicializando Plano de Contas PGC Angolano...");
        
        List<PlanoConta> contas = criarEstruturaPGC();
        for (PlanoConta conta : contas) {
            planoContaService.save(conta);
        }
        
        System.out.println("Plano de contas PGC Angolano inicializado com sucesso! Total: " + contas.size() + " contas.");
    }

    /**
     * Cria a estrutura completa do PGC Angolano.
     */
    private List<PlanoConta> criarEstruturaPGC() {
        List<PlanoConta> contas = new ArrayList<>();
        
        // =====================================================
        // CLASSE 1 - CONTAS FINANCEIRAS (Ativos Financeiros)
        // =====================================================
        contas.add(criarConta("1", "Meios Financeiros", ClasseConta.CLASSE_1, NaturezaConta.DEVEDORA, 1, false, null));
        
        // 11 - Caixa e Equivalentes de Caixa
        contas.add(criarConta("11", "Caixa", ClasseConta.CLASSE_1, NaturezaConta.DEVEDORA, 2, false, "1"));
        contas.add(criarConta("11.1", "Caixa Geral", ClasseConta.CLASSE_1, NaturezaConta.DEVEDORA, 3, true, "11"));
        contas.add(criarConta("11.2", "Caixa Pdv 1", ClasseConta.CLASSE_1, NaturezaConta.DEVEDORA, 3, true, "11"));
        contas.add(criarConta("11.3", "Caixa Pdv 2", ClasseConta.CLASSE_1, NaturezaConta.DEVEDORA, 3, true, "11"));
        
        // 12 - Depósitos Bancários à Ordem
        contas.add(criarConta("12", "Depósitos Bancários", ClasseConta.CLASSE_1, NaturezaConta.DEVEDORA, 2, false, "1"));
        contas.add(criarConta("12.1", "Banco Principal", ClasseConta.CLASSE_1, NaturezaConta.DEVEDORA, 3, true, "12"));
        contas.add(criarConta("12.2", "Banco Secundário", ClasseConta.CLASSE_1, NaturezaConta.DEVEDORA, 3, true, "12"));
        
        // =====================================================
        // CLASSE 2 - CONTAS DE MOVIMENTO E ARMAZÉM
        // =====================================================
        contas.add(criarConta("2", "Contas de Movimento e Armazém", ClasseConta.CLASSE_2, NaturezaConta.DEVEDORA, 1, false, null));
        
        // 21 - Compras
        contas.add(criarConta("21", "Compras", ClasseConta.CLASSE_2, NaturezaConta.DEVEDORA, 2, false, "2"));
        contas.add(criarConta("21.1", "Compras de Mercadorias", ClasseConta.CLASSE_2, NaturezaConta.DEVEDORA, 3, true, "21"));
        contas.add(criarConta("21.2", "Compras de Materias Primas", ClasseConta.CLASSE_2, NaturezaConta.DEVEDORA, 3, true, "21"));
        contas.add(criarConta("21.3", "Compras de Subsidiárias", ClasseConta.CLASSE_2, NaturezaConta.DEVEDORA, 3, true, "21"));
        
        // 22 - Mercadorias (Inventário)
        contas.add(criarConta("22", "Mercadorias", ClasseConta.CLASSE_2, NaturezaConta.DEVEDORA, 2, false, "2"));
        contas.add(criarConta("22.1", "Mercadorias - Armazém Principal", ClasseConta.CLASSE_2, NaturezaConta.DEVEDORA, 3, true, "22"));
        contas.add(criarConta("22.2", "Mercadorias - Loja 1", ClasseConta.CLASSE_2, NaturezaConta.DEVEDORA, 3, true, "22"));
        contas.add(criarConta("22.3", "Produtos Acabados", ClasseConta.CLASSE_2, NaturezaConta.DEVEDORA, 3, true, "22"));
        
        // 24 - IVA Dedução
        contas.add(criarConta("24", "Imposto sobre o Valor Acrescentado", ClasseConta.CLASSE_2, NaturezaConta.DEVEDORA, 2, false, "2"));
        contas.add(criarConta("24.3", "IVA Dedução", ClasseConta.CLASSE_2, NaturezaConta.DEVEDORA, 3, false, "24"));
        contas.add(criarConta("24.3.1", "IVA Dedução Suportado 7%", ClasseConta.CLASSE_2, NaturezaConta.DEVEDORA, 4, true, "24.3"));
        contas.add(criarConta("24.3.2", "IVA Dedução Suportado 14%", ClasseConta.CLASSE_2, NaturezaConta.DEVEDORA, 4, true, "24.3"));
        contas.add(criarConta("24.3.9", "IVA Dedução Outras Taxas", ClasseConta.CLASSE_2, NaturezaConta.DEVEDORA, 4, true, "24.3"));
        
        // =====================================================
        // CLASSE 3 - CONTAS DE TERCEIROS (Clientes e Fornecedores)
        // =====================================================
        contas.add(criarConta("3", "Contas de Terceiros", ClasseConta.CLASSE_3, NaturezaConta.DEVEDORA, 1, false, null));
        
        // 31 - Clientes
        contas.add(criarConta("31", "Clientes", ClasseConta.CLASSE_3, NaturezaConta.DEVEDORA, 2, false, "3"));
        contas.add(criarConta("31.1", "Clientes - Contas a Receber", ClasseConta.CLASSE_3, NaturezaConta.DEVEDORA, 3, true, "31"));
        contas.add(criarConta("31.2", "Clientes - Dúvidas de Cobrança", ClasseConta.CLASSE_3, NaturezaConta.DEVEDORA, 3, true, "31"));
        
        // 32 - Fornecedores
        contas.add(criarConta("32", "Fornecedores", ClasseConta.CLASSE_3, NaturezaConta.CREDORA, 2, false, "3"));
        contas.add(criarConta("32.1", "Fornecedores - Contas a Pagar", ClasseConta.CLASSE_3, NaturezaConta.CREDORA, 3, true, "32"));
        contas.add(criarConta("32.2", "Fornecedores - Factoring", ClasseConta.CLASSE_3, NaturezaConta.CREDORA, 3, true, "32"));
        
        // 33 - Estado e Outros Entes Públicos (IVA a pagar)
        contas.add(criarConta("33", "Estado e Outros Entes Públicos", ClasseConta.CLASSE_3, NaturezaConta.CREDORA, 2, false, "3"));
        contas.add(criarConta("33.1", "IVA a Pagar", ClasseConta.CLASSE_3, NaturezaConta.CREDORA, 3, false, "33"));
        contas.add(criarConta("33.1.1", "IVA a Pagar 7%", ClasseConta.CLASSE_3, NaturezaConta.CREDORA, 4, true, "33.1"));
        contas.add(criarConta("33.1.2", "IVA a Pagar 14%", ClasseConta.CLASSE_3, NaturezaConta.CREDORA, 4, true, "33.1"));
        contas.add(criarConta("33.1.3", "IVA Isento", ClasseConta.CLASSE_3, NaturezaConta.CREDORA, 4, true, "33.1"));
        contas.add(criarConta("33.8", "Retenções na Fonte", ClasseConta.CLASSE_3, NaturezaConta.CREDORA, 3, true, "33"));
        
        // =====================================================
        // CLASSE 4 - CONTAS DE PESSOAL
        // =====================================================
        contas.add(criarConta("4", "Contas de Pessoal", ClasseConta.CLASSE_4, NaturezaConta.DEVEDORA, 1, false, null));
        
        // 42 - Remunerações a Pagar
        contas.add(criarConta("42", "Remunerações a Pagar", ClasseConta.CLASSE_4, NaturezaConta.CREDORA, 2, false, "4"));
        contas.add(criarConta("42.1", "Salários por Pagar", ClasseConta.CLASSE_4, NaturezaConta.CREDORA, 3, true, "42"));
        contas.add(criarConta("42.2", "Subsídios por Pagar", ClasseConta.CLASSE_4, NaturezaConta.CREDORA, 3, true, "42"));
        
        // =====================================================
        // CLASSE 5 - CONTAS DE CAPITAL, RESERVAS E PASSIVO
        // =====================================================
        contas.add(criarConta("5", "Contas de Capital, Reservas e Passivo", ClasseConta.CLASSE_5, NaturezaConta.DEVEDORA, 1, false, null));

        // 51 - Capital Social
        contas.add(criarConta("51", "Capital Social", ClasseConta.CLASSE_5, NaturezaConta.CREDORA, 2, false, "5"));
        contas.add(criarConta("51.1", "Capital Realizado", ClasseConta.CLASSE_5, NaturezaConta.CREDORA, 3, true, "51"));
        contas.add(criarConta("51.2", "Capital Subscrito", ClasseConta.CLASSE_5, NaturezaConta.CREDORA, 3, true, "51"));

        // 52 - Reservas
        contas.add(criarConta("52", "Reservas", ClasseConta.CLASSE_5, NaturezaConta.CREDORA, 2, false, "5"));
        contas.add(criarConta("52.1", "Reserva Legal", ClasseConta.CLASSE_5, NaturezaConta.CREDORA, 3, true, "52"));
        contas.add(criarConta("52.2", "Reservas Estatutárias", ClasseConta.CLASSE_5, NaturezaConta.CREDORA, 3, true, "52"));

        // 53 - Financiamentos Obtidos
        contas.add(criarConta("53", "Financiamentos Obtidos", ClasseConta.CLASSE_5, NaturezaConta.CREDORA, 2, false, "5"));
        contas.add(criarConta("53.1", "Empréstimos Bancários", ClasseConta.CLASSE_5, NaturezaConta.CREDORA, 3, true, "53"));
        contas.add(criarConta("53.2", "Financiamentos com Partes Relacionadas", ClasseConta.CLASSE_5, NaturezaConta.CREDORA, 3, true, "53"));
        
        // =====================================================
        // CLASSE 6 - PROVEITOS (Receitas)
        // =====================================================
        contas.add(criarConta("6", "Proveitos", ClasseConta.CLASSE_6, NaturezaConta.CREDORA, 1, false, null));
        
        // 61 - Vendas
        contas.add(criarConta("61", "Vendas", ClasseConta.CLASSE_6, NaturezaConta.CREDORA, 2, false, "6"));
        contas.add(criarConta("61.1", "Vendas de Mercadorias", ClasseConta.CLASSE_6, NaturezaConta.CREDORA, 3, true, "61"));
        contas.add(criarConta("61.2", "Vendas de Produtos Acabados", ClasseConta.CLASSE_6, NaturezaConta.CREDORA, 3, true, "61"));
        contas.add(criarConta("61.3", "Vendas de Serviços", ClasseConta.CLASSE_6, NaturezaConta.CREDORA, 3, true, "61"));
        
        // 62 - Prestações de Serviços
        contas.add(criarConta("62", "Prestações de Serviços", ClasseConta.CLASSE_6, NaturezaConta.CREDORA, 2, false, "6"));
        contas.add(criarConta("62.1", "Prestação de Serviços - Consultoria", ClasseConta.CLASSE_6, NaturezaConta.CREDORA, 3, true, "62"));
        
        // =====================================================
        // CLASSE 7 - CUSTOS (Despesas)
        // =====================================================
        contas.add(criarConta("7", "Custos", ClasseConta.CLASSE_7, NaturezaConta.DEVEDORA, 1, false, null));
        
        // 71 - Custo das Mercadorias Vendidas (CMV)
        contas.add(criarConta("71", "Custo das Mercadorias Vendidas", ClasseConta.CLASSE_7, NaturezaConta.DEVEDORA, 2, false, "7"));
        contas.add(criarConta("71.1", "Custo das Mercadorias Vendidas", ClasseConta.CLASSE_7, NaturezaConta.DEVEDORA, 3, true, "71"));
        
        // 72 - Fornecimentos e Serviços Externos
        contas.add(criarConta("72", "Fornecimentos e Serviços Externos", ClasseConta.CLASSE_7, NaturezaConta.DEVEDORA, 2, false, "7"));
        contas.add(criarConta("72.1", "Água e Electricidade", ClasseConta.CLASSE_7, NaturezaConta.DEVEDORA, 3, true, "72"));
        contas.add(criarConta("72.2", "Comunicações", ClasseConta.CLASSE_7, NaturezaConta.DEVEDORA, 3, true, "72"));
        contas.add(criarConta("72.3", "Alugueres", ClasseConta.CLASSE_7, NaturezaConta.DEVEDORA, 3, true, "72"));
        
        // 73 - Despesas com o Pessoal
        contas.add(criarConta("73", "Despesas com o Pessoal", ClasseConta.CLASSE_7, NaturezaConta.DEVEDORA, 2, false, "7"));
        contas.add(criarConta("73.1", "Salários e Ordenados", ClasseConta.CLASSE_7, NaturezaConta.DEVEDORA, 3, true, "73"));
        contas.add(criarConta("73.2", "Segurança Social", ClasseConta.CLASSE_7, NaturezaConta.DEVEDORA, 3, true, "73"));
        contas.add(criarConta("73.3", "Subsídios", ClasseConta.CLASSE_7, NaturezaConta.DEVEDORA, 3, true, "73"));
        
        // 74 - Amortizações do Exercício
        contas.add(criarConta("74", "Amortizações do Exercício", ClasseConta.CLASSE_7, NaturezaConta.DEVEDORA, 2, false, "7"));
        contas.add(criarConta("74.2", "Amortizações de Equipamentos", ClasseConta.CLASSE_7, NaturezaConta.DEVEDORA, 3, true, "74"));
        contas.add(criarConta("74.3", "Amortizações de Mobiliário", ClasseConta.CLASSE_7, NaturezaConta.DEVEDORA, 3, true, "74"));
        
        // 76 - Outros Custos e Perdas
        contas.add(criarConta("76", "Outros Custos e Perdas", ClasseConta.CLASSE_7, NaturezaConta.DEVEDORA, 2, false, "7"));
        contas.add(criarConta("76.1", "Impostos e Taxas", ClasseConta.CLASSE_7, NaturezaConta.DEVEDORA, 3, true, "76"));
        
        // =====================================================
        // CLASSE 8 - RESULTADOS E EQUILÍBRIO PATRIMONIAL
        // =====================================================
        contas.add(criarConta("8", "Resultados e Equilíbrio Patrimonial", ClasseConta.CLASSE_8, NaturezaConta.DEVEDORA, 1, false, null));
        
        // 81 - Resultados Transitados
        contas.add(criarConta("81", "Resultados Transitados", ClasseConta.CLASSE_8, NaturezaConta.CREDORA, 2, false, "8"));
        contas.add(criarConta("81.1", "Resultados Transitados (Lucros)", ClasseConta.CLASSE_8, NaturezaConta.CREDORA, 3, true, "81"));
        contas.add(criarConta("81.2", "Resultados Transitados (Prejuízos)", ClasseConta.CLASSE_8, NaturezaConta.DEVEDORA, 3, true, "81"));
        
        // 82 - Resultado do Exercício
        contas.add(criarConta("82", "Resultado do Exercício", ClasseConta.CLASSE_8, NaturezaConta.CREDORA, 2, false, "8"));
        contas.add(criarConta("82.1", "Resultado do Exercício", ClasseConta.CLASSE_8, NaturezaConta.CREDORA, 3, true, "82"));
        
        // 84 - Dividendos a Distribuir
        contas.add(criarConta("84", "Dividendos a Distribuir", ClasseConta.CLASSE_8, NaturezaConta.DEVEDORA, 2, false, "8"));
        contas.add(criarConta("84.1", "Dividendos Antecipados", ClasseConta.CLASSE_8, NaturezaConta.DEVEDORA, 3, true, "84"));
        
        return contas;
    }

    private PlanoConta criarConta(String codigo, String descricao, ClasseConta classe, 
                                   NaturezaConta natureza, Integer nivel, 
                                   Boolean movimento, String codigoPai) {
        PlanoConta conta = new PlanoConta();
        conta.setCodigo(codigo);
        conta.setDescricao(descricao);
        conta.setClasse(classe);
        conta.setNatureza(natureza);
        conta.setNivel(nivel);
        conta.setMovimento(movimento);
        
        // O pai será configurado pelo serviço PlanoContaService durante o save
        // através da lógica de hierarquia
        
        return conta;
    }

    /**
     * Verifica se o plano de contas está configurado corretamente.
     * Retorna uma lista de problemas encontrados.
     */
    public List<String> validarEstruturaPGC() {
        List<String> problemas = new ArrayList<>();
        
        // Verificar se todas as classes existem
        for (ClasseConta classe : ClasseConta.values()) {
            boolean existe = planoContaRepository.findAll().stream()
                    .anyMatch(c -> c.getClasse() == classe);
            if (!existe) {
                problemas.add("Classe " + classe + " não encontrada no plano de contas");
            }
        }
        
        // Verificar se contas de IVA estão configuradas
        boolean temIVADeducao = planoContaRepository.findByCodigo("24.3").isPresent();
        boolean temIVAPagar = planoContaRepository.findByCodigo("33.1").isPresent();
        
        if (!temIVADeducao) {
            problemas.add("Conta 24.3 (IVA Dedução) não encontrada - necessária para faturação");
        }
        if (!temIVAPagar) {
            problemas.add("Conta 33.1 (IVA a Pagar) não encontrada - necessária para faturação");
        }
        
        // Verificar se contas de vendas existem
        boolean temVendas = planoContaRepository.findByCodigo("61").isPresent();
        if (!temVendas) {
            problemas.add("Conta 61 (Vendas) não encontrada - necessária para faturação");
        }
        
        return problemas;
    }
}
