package ao.allon.kubata.rh.domain;

import ao.allon.kubata.core.domain.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "rh_periodo_ferias",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_periodo_ferias_colaborador_ano", 
                           columnNames = {"colaborador_id", "ano"})
       })
public class PeriodoFerias extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "colaborador_id", nullable = false)
    private Colaborador colaborador;

    @NotNull(message = "Ano é obrigatório")
    @Min(value = 2020, message = "Ano deve ser >= 2020")
    @Column(name = "ano", nullable = false)
    private Integer ano;

    @Column(name = "dias_direito", nullable = false)
    private Integer diasDireito = 22; // Padrão Angola: 22 dias úteis

    @Column(name = "dias_usados", nullable = false)
    private Integer diasUsados = 0;

    @Column(name = "dias_saldo", nullable = false)
    private Integer diasSaldo;

    @Column(name = "dias_proporcionais", nullable = false)
    private Integer diasProporcionais = 0;

    @Size(max = 500, message = "Observações não podem exceder 500 caracteres")
    @Column(name = "observacoes", length = 500)
    private String observacoes;

    public PeriodoFerias() {
        // Inicializa saldo automaticamente
        this.diasSaldo = this.diasDireito;
    }

    public PeriodoFerias(Colaborador colaborador, Integer ano) {
        this();
        this.colaborador = colaborador;
        this.ano = ano;
    }

    public Colaborador getColaborador() {
        return colaborador;
    }

    public void setColaborador(Colaborador colaborador) {
        this.colaborador = colaborador;
    }

    public Integer getAno() {
        return ano;
    }

    public void setAno(Integer ano) {
        this.ano = ano;
    }

    public Integer getDiasDireito() {
        return diasDireito;
    }

    public void setDiasDireito(Integer diasDireito) {
        this.diasDireito = diasDireito;
        atualizarSaldo();
    }

    public Integer getDiasUsados() {
        return diasUsados;
    }

    public void setDiasUsados(Integer diasUsados) {
        this.diasUsados = diasUsados;
        atualizarSaldo();
    }

    public Integer getDiasSaldo() {
        return diasSaldo;
    }

    public void setDiasSaldo(Integer diasSaldo) {
        this.diasSaldo = diasSaldo;
    }

    public Integer getDiasProporcionais() {
        return diasProporcionais;
    }

    public void setDiasProporcionais(Integer diasProporcionais) {
        this.diasProporcionais = diasProporcionais;
        atualizarSaldo();
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    /**
     * Atualiza o saldo de dias disponíveis
     */
    public void atualizarSaldo() {
        int totalDisponivel = (diasDireito != null ? diasDireito : 0) + 
                             (diasProporcionais != null ? diasProporcionais : 0);
        this.diasSaldo = totalDisponivel - (diasUsados != null ? diasUsados : 0);
        if (this.diasSaldo < 0) {
            this.diasSaldo = 0;
        }
    }

    /**
     * Adiciona dias usados (usado quando um pedido é aprovado)
     */
    public void adicionarDiasUsados(Integer dias) {
        if (dias != null && dias > 0) {
            this.diasUsados = (this.diasUsados != null ? this.diasUsados : 0) + dias;
            atualizarSaldo();
        }
    }

    /**
     * Remove dias usados (usado quando um pedido é cancelado)
     */
    public void removerDiasUsados(Integer dias) {
        if (dias != null && dias > 0) {
            this.diasUsados = Math.max(0, (this.diasUsados != null ? this.diasUsados : 0) - dias);
            atualizarSaldo();
        }
    }

    @Override
    public String toString() {
        return colaborador != null ? colaborador.getNomeCompleto() + " - " + ano : "PeriodoFerias";
    }
}
