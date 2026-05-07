package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import ao.allon.kubata.faturacao.repository.FaturaRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class RelatorioService {

    private final FaturaRepository faturaRepository;

    public RelatorioService(FaturaRepository faturaRepository) {
        this.faturaRepository = faturaRepository;
    }

    public static class RelatorioVendasClientes {
        private LocalDate dataInicio;
        private LocalDate dataFim;
        
        private BigDecimal totalVendasConsumidorFinal;
        private Long qtdVendasConsumidorFinal;
        
        private BigDecimal totalVendasClientesCadastrados;
        private Long qtdVendasClientesCadastrados;

        public LocalDate getDataInicio() {
            return dataInicio;
        }

        public void setDataInicio(LocalDate dataInicio) {
            this.dataInicio = dataInicio;
        }

        public LocalDate getDataFim() {
            return dataFim;
        }

        public void setDataFim(LocalDate dataFim) {
            this.dataFim = dataFim;
        }

        public BigDecimal getTotalVendasConsumidorFinal() {
            return totalVendasConsumidorFinal;
        }

        public void setTotalVendasConsumidorFinal(BigDecimal totalVendasConsumidorFinal) {
            this.totalVendasConsumidorFinal = totalVendasConsumidorFinal;
        }

        public Long getQtdVendasConsumidorFinal() {
            return qtdVendasConsumidorFinal;
        }

        public void setQtdVendasConsumidorFinal(Long qtdVendasConsumidorFinal) {
            this.qtdVendasConsumidorFinal = qtdVendasConsumidorFinal;
        }

        public BigDecimal getTotalVendasClientesCadastrados() {
            return totalVendasClientesCadastrados;
        }

        public void setTotalVendasClientesCadastrados(BigDecimal totalVendasClientesCadastrados) {
            this.totalVendasClientesCadastrados = totalVendasClientesCadastrados;
        }

        public Long getQtdVendasClientesCadastrados() {
            return qtdVendasClientesCadastrados;
        }

        public void setQtdVendasClientesCadastrados(Long qtdVendasClientesCadastrados) {
            this.qtdVendasClientesCadastrados = qtdVendasClientesCadastrados;
        }
        
        public BigDecimal getTotalGeral() {
            return totalVendasConsumidorFinal.add(totalVendasClientesCadastrados);
        }
        
        public Long getQtdGeral() {
            return qtdVendasConsumidorFinal + qtdVendasClientesCadastrados;
        }
    }

    public RelatorioVendasClientes gerarRelatorioVendasPorTipoCliente(LocalDate inicio, LocalDate fim) {
        RelatorioVendasClientes relatorio = new RelatorioVendasClientes();
        relatorio.setDataInicio(inicio);
        relatorio.setDataFim(fim);

        String nomeConsumidorFinal = "Consumidor Final";

        // Consumidor Final
        BigDecimal totalAnonimo = faturaRepository.sumTotalByStatusAndDataEmissaoBetweenAndClienteNome(
                StatusFatura.EMITIDA, inicio, fim, nomeConsumidorFinal);
        Long qtdAnonimo = faturaRepository.countByStatusAndDataEmissaoBetweenAndClienteNome(
                StatusFatura.EMITIDA, inicio, fim, nomeConsumidorFinal);

        // Clientes Cadastrados (Todos que não são Consumidor Final)
        BigDecimal totalCadastrados = faturaRepository.sumTotalByStatusAndDataEmissaoBetweenAndClienteNomeNot(
                StatusFatura.EMITIDA, inicio, fim, nomeConsumidorFinal);
        Long qtdCadastrados = faturaRepository.countByStatusAndDataEmissaoBetweenAndClienteNomeNot(
                StatusFatura.EMITIDA, inicio, fim, nomeConsumidorFinal);

        relatorio.setTotalVendasConsumidorFinal(totalAnonimo != null ? totalAnonimo : BigDecimal.ZERO);
        relatorio.setQtdVendasConsumidorFinal(qtdAnonimo != null ? qtdAnonimo : 0L);
        
        relatorio.setTotalVendasClientesCadastrados(totalCadastrados != null ? totalCadastrados : BigDecimal.ZERO);
        relatorio.setQtdVendasClientesCadastrados(qtdCadastrados != null ? qtdCadastrados : 0L);

        return relatorio;
    }
}
