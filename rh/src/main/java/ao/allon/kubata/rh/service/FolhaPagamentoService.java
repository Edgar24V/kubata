package ao.allon.kubata.rh.service;

import ao.allon.kubata.core.domain.PlanoConta;
import ao.allon.kubata.core.repository.PlanoContaRepository;
import ao.allon.kubata.core.service.ContabilidadeService;
import ao.allon.kubata.rh.domain.Colaborador;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class FolhaPagamentoService {

    private final ContabilidadeService contabilidadeService;
    private final PlanoContaRepository planoContaRepository;
    private final ColaboradorService colaboradorService;

    public FolhaPagamentoService(ContabilidadeService contabilidadeService, 
                                 PlanoContaRepository planoContaRepository, 
                                 ColaboradorService colaboradorService) {
        this.contabilidadeService = contabilidadeService;
        this.planoContaRepository = planoContaRepository;
        this.colaboradorService = colaboradorService;
    }

    /**
     * Processa a folha de pagamento e gera lançamentos automáticos na contabilidade única.
     * D: 75.1 - Custos com Pessoal
     * C: 36.1 - Pessoal (Remunerações a Pagar)
     */
    @Transactional
    public void processarFolha(int mes, int ano) {
        List<Colaborador> colaboradores = colaboradorService.findAtivos();
        BigDecimal totalSalarios = BigDecimal.ZERO;

        for (Colaborador c : colaboradores) {
            if (c.getSalarioBase() != null) {
                totalSalarios = totalSalarios.add(c.getSalarioBase());
            }
        }

        if (totalSalarios.compareTo(BigDecimal.ZERO) > 0) {
            PlanoConta contaCustos = planoContaRepository.findByCodigo("75.1")
                    .orElseThrow(() -> new IllegalStateException("Conta 75.1 (Custos com Pessoal) não encontrada no plano único."));
            PlanoConta contaPessoal = planoContaRepository.findByCodigo("36.1")
                    .orElseThrow(() -> new IllegalStateException("Conta 36.1 (Pessoal) não encontrada no plano único."));

            contabilidadeService.lancar(
                    LocalDate.now(),
                    contaCustos,
                    contaPessoal,
                    totalSalarios,
                    "Processamento de Salários - Mês " + mes + "/" + ano,
                    "FOLHA-" + ano + "-" + mes,
                    "RH_SALARIOS",
                    null
            );
        }
    }
}
